import cv2
import mediapipe as mp
import math

DETECT_ERROR_SCORE = 99.9

LANDMARK_SPECS = {
    "right_pupil": 468, "left_pupil": 473,
    "left_eyebrow_inner": 55 , "right_eyebrow_inner": 285,
    "left_eye_p1": 33, "left_eye_p2": 160, "left_eye_p3": 158,
    "left_eye_p4": 133, "left_eye_p5": 153, "left_eye_p6": 144,
    "right_eye_p1": 362, "right_eye_p2": 385, "right_eye_p3": 387,
    "right_eye_p4": 263, "right_eye_p5": 380, "right_eye_p6": 373
}

LEFT_EYE_INDICES = [
    LANDMARK_SPECS["left_eye_p1"], LANDMARK_SPECS["left_eye_p2"],
    LANDMARK_SPECS["left_eye_p3"], LANDMARK_SPECS["left_eye_p4"],
    LANDMARK_SPECS["left_eye_p5"], LANDMARK_SPECS["left_eye_p6"]
]

RIGHT_EYE_INDICES = [
    LANDMARK_SPECS["right_eye_p1"], LANDMARK_SPECS["right_eye_p2"],
    LANDMARK_SPECS["right_eye_p3"], LANDMARK_SPECS["right_eye_p4"],
    LANDMARK_SPECS["right_eye_p5"], LANDMARK_SPECS["right_eye_p6"]
]


CONFIG = {
    # [수정됨] 픽셀 단위(7.0)에서 비율 단위(0.2)로 변경
    # 눈이 떠진 비율(EAR)이 0.2 이하일 때 찡그림(Squint)으로 판단합니다.
    "SQUINT_EAR_THRESHOLD": 0.2, 
    
    "FROWN_RATIO_THRESHOLD": 0.1,
    "GAZE_SIDE_THRESHOLD": 0.6,
    "SQUINT_DURATION_THRESHOLD": 0.1,
    "LISTENING_TENSE_DURATION": 0.1, "SPEAKING_TENSE_DURATION": 0.1,
    "LISTENING_GAZE_DURATION": 0.1, "SPEAKING_GAZE_DURATION": 0.1,
    "FROWN_NORM_MAX": 0.1, "SQUINT_NORM_MAX": 0.15, "GAZE_NORM_MAX": 1.0,
    "WEIGHT_TENSION": 0.4, "WEIGHT_GAZE": 0.6
}

class FaceMeshAnalyzer:

    def __init__(self, config, landmark_specs, left_eye_indices, right_eye_indices):
        # --- FaceMesh (정밀 분석) ---
        self.mp_face_mesh = mp.solutions.face_mesh
        self.face_mesh = self.mp_face_mesh.FaceMesh(
            static_image_mode=True,
            max_num_faces=1,
            refine_landmarks=True,
            min_detection_confidence=0.3
        )

        self.config = config
        self.landmark_specs = landmark_specs
        self.left_eye_indices = left_eye_indices
        self.right_eye_indices = right_eye_indices

        self.expression_start_time = {}
        self.current_state = "SPEAKING"
        self.feedback_message = ""

        self.current_frown_degree = DETECT_ERROR_SCORE
        self.current_squint_degree = DETECT_ERROR_SCORE
        self.current_gaze_degree = DETECT_ERROR_SCORE

        self.normalized_frown_degree = 0.0
        self.normalized_squint_degree = 0.0
        self.normalized_gaze_degree = 0.0

        self.comprehensive_score = 100.0
        self.frame_counter = 0
        self.last_known_bbox = None

        self.current_frame_width = 0
        self.current_frame_height = 0



    def set_frame_size(self, width, height):
        """외부에서 프레임 크기 설정"""
        self.current_frame_width = width
        self.current_frame_height = height

    def close(self):
        """모델 리소스 해제"""
        self.face_mesh.close()



    def process_frame(self, frame, timestamp_ms, frame_number=None, face_bbox=None):
        """
        프레임 분석 수행
        
        Args:
            frame: BGR 이미지 프레임
            timestamp_ms: 타임스탬프 (밀리초)
            frame_number: 외부에서 관리하는 프레임 번호 (옵션)
            face_bbox: main에서 전달받은 얼굴 bbox (fx1, fy1, fx2, fy2)
            
        Returns:
            dict: 분석 결과 데이터 (bool 딕셔너리)
        """
        if frame_number is not None:
            self.frame_counter = frame_number
        
        self.feedback_message = ""
        
        self._process_frame_hybrid(frame, face_bbox)
        self._update_feedback_logic(timestamp_ms)
        
        # bool 딕셔너리로 반환
        result = {
            'gaze_averted': False,
            'frown': False,
            'squint': False,
            'face_not_detected': False
        }
        
        issues = []  # 발생한 문제 목록
        
        if self.feedback_message == "CANNOT detect face":
            result['face_not_detected'] = True
            issues.append('face_not_detected')
        elif self.feedback_message:
            if "gaze" in self.feedback_message.lower():
                result['gaze_averted'] = True
                issues.append('gaze_averted')
            if "frown" in self.feedback_message.lower():
                result['frown'] = True
                issues.append('frown')
            if "squint" in self.feedback_message.lower():
                result['squint'] = True
                issues.append('squint')
        
        result['issues'] = issues
        return result



    # 랜드마크 좌표 변환 함수
    @staticmethod
    def _get_landmark(landmarks, index, roi_w, roi_h, fx1, fy1):
        try:
            landmark = landmarks[index]
            global_x = (landmark.x * roi_w) + fx1
            global_y = (landmark.y * roi_h) + fy1
            return global_x, global_y
        except (IndexError, TypeError):
            return None



    # 두 점의 유클리드 거리 계산 함수
    @staticmethod
    def _distance(p1, p2):
        if p1 is None or p2 is None:
            return 0
        return math.sqrt((p1[0] - p2[0]) ** 2 + (p1[1] - p2[1]) ** 2)



    # [수정됨] 눈의 가로세로 비율(EAR) 계산
    @staticmethod
    def _calculate_eye_aspect_ratio(landmarks, eye_indices, roi_w, roi_h, fx1, fy1):
        p = [FaceMeshAnalyzer._get_landmark(landmarks, idx, roi_w, roi_h, fx1, fy1) for idx in eye_indices]
        if any(v is None for v in p):
            return DETECT_ERROR_SCORE
        
        # 세로 거리 (눈꺼풀 사이 거리)
        vertical_dist1 = FaceMeshAnalyzer._distance(p[1], p[5])
        vertical_dist2 = FaceMeshAnalyzer._distance(p[2], p[4])
        
        # 가로 거리 (눈의 양 끝점 거리) - p[0]: 왼쪽 끝, p[3]: 오른쪽 끝
        horizontal_dist = FaceMeshAnalyzer._distance(p[0], p[3])
        
        if horizontal_dist == 0:
            return DETECT_ERROR_SCORE
            
        avg_vertical = (vertical_dist1 + vertical_dist2) / 2.0
        
        # EAR(Eye Aspect Ratio) = 세로 평균 / 가로
        # 값이 작을수록 눈을 감거나 찡그린 상태입니다.
        return avg_vertical / horizontal_dist




    @staticmethod
    def _calculate_frown_ratio(frown_dist, face_dist):
        """미간 수축 비율 계산"""
        if not face_dist or not frown_dist or face_dist == 0:
            return 99.0
        return frown_dist / face_dist



    # 시선 이탈 비율 계산
    @staticmethod
    def _calculate_gaze_ratio(landmarks, eye_indices, pupil_index, roi_w, roi_h, fx1, fy1):
        pupil = FaceMeshAnalyzer._get_landmark(landmarks, pupil_index, roi_w, roi_h, fx1, fy1)
        p1 = FaceMeshAnalyzer._get_landmark(landmarks, eye_indices[0], roi_w, roi_h, fx1, fy1)
        p4 = FaceMeshAnalyzer._get_landmark(landmarks, eye_indices[3], roi_w, roi_h, fx1, fy1)
        p2 = FaceMeshAnalyzer._get_landmark(landmarks, eye_indices[1], roi_w, roi_h, fx1, fy1)
        p5 = FaceMeshAnalyzer._get_landmark(landmarks, eye_indices[4], roi_w, roi_h, fx1, fy1)

        if any(v is None for v in [pupil, p1, p4, p2, p5]):
            return DETECT_ERROR_SCORE, DETECT_ERROR_SCORE

        eye_width = FaceMeshAnalyzer._distance(p1, p4)
        eye_height = FaceMeshAnalyzer._distance(p2, p5)

        if not eye_width or not eye_height or eye_width > 99.0 or eye_height > 99.0:
            return DETECT_ERROR_SCORE, DETECT_ERROR_SCORE

        eye_center_x = (p1[0] + p4[0]) / 2.0
        eye_center_y = (p2[1] + p5[1]) / 2.0

        gaze_x_ratio = (pupil[0] - eye_center_x) / (eye_width / 2.0)
        gaze_y_ratio = (pupil[1] - eye_center_y) / (eye_height / 2.0)

        return gaze_x_ratio, gaze_y_ratio





    def _process_frame_hybrid(self, frame, face_bbox=None):
        self.current_frown_degree = DETECT_ERROR_SCORE
        self.current_squint_degree = DETECT_ERROR_SCORE
        self.current_gaze_degree = DETECT_ERROR_SCORE

        self.normalized_frown_degree = 0.0
        self.normalized_squint_degree = 0.0
        self.normalized_gaze_degree = 0.0

        current_bbox = face_bbox

        if current_bbox:
            self.last_known_bbox = current_bbox
        else:
            self.last_known_bbox = None

        if current_bbox:
            fx1, fy1, fx2, fy2 = current_bbox
            fx1, fy1 = max(0, fx1), max(0, fy1)
            fx2, fy2 = min(self.current_frame_width, fx2), min(self.current_frame_height, fy2)

            face_roi = frame[fy1:fy2, fx1:fx2].copy()

            if face_roi.size > 0:
                face_roi_rgb = cv2.cvtColor(face_roi, cv2.COLOR_BGR2RGB)
                roi_h, roi_w, _ = face_roi_rgb.shape

                face_roi_rgb.flags.writeable = False
                mp_results = self.face_mesh.process(face_roi_rgb)
                face_roi_rgb.flags.writeable = True

                if mp_results.multi_face_landmarks:
                    face_landmarks = mp_results.multi_face_landmarks[0].landmark

                    # 시선 계산
                    left_gaze_x, left_gaze_y = self._calculate_gaze_ratio(
                        face_landmarks, self.left_eye_indices, self.landmark_specs["left_pupil"], roi_w, roi_h, fx1, fy1
                    )
                    right_gaze_x, right_gaze_y = self._calculate_gaze_ratio(
                        face_landmarks, self.right_eye_indices, self.landmark_specs["right_pupil"], roi_w, roi_h, fx1, fy1
                    )

                    avg_gaze_x = (left_gaze_x + right_gaze_x) / 2.0
                    avg_gaze_y = (left_gaze_y + right_gaze_y) / 2.0
                    self.current_gaze_degree = abs(avg_gaze_x) + abs(avg_gaze_y)

                    # 눈 가늘게 뜨기 계산 (EAR 비율)
                    left_ear = self._calculate_eye_aspect_ratio(face_landmarks, self.left_eye_indices, roi_w, roi_h, fx1, fy1)
                    right_ear = self._calculate_eye_aspect_ratio(face_landmarks, self.right_eye_indices, roi_w, roi_h, fx1, fy1)
                    self.current_squint_degree = (left_ear + right_ear) / 2.0

                    # 미간 찌푸림 계산
                    left_eyebrow = self._get_landmark(face_landmarks, self.landmark_specs["left_eyebrow_inner"], roi_w, roi_h, fx1, fy1)
                    right_eyebrow = self._get_landmark(face_landmarks, self.landmark_specs["right_eyebrow_inner"], roi_w, roi_h, fx1, fy1)
                    eyebrow_dist = self._distance(left_eyebrow, right_eyebrow)

                    face_left = self._get_landmark(face_landmarks, self.landmark_specs["left_eye_p1"], roi_w, roi_h, fx1, fy1)
                    face_right = self._get_landmark(face_landmarks, self.landmark_specs["right_eye_p4"], roi_w, roi_h, fx1, fy1)
                    face_dist = self._distance(face_left, face_right)

                    self.current_frown_degree = self._calculate_frown_ratio(eyebrow_dist, face_dist)

        # 정규화 및 임계값 비교
        if self.current_frown_degree < 99.0:
            self.normalized_frown_degree = max(0.0, self.config["FROWN_RATIO_THRESHOLD"] - self.current_frown_degree)
        else:
            self.normalized_frown_degree = 0.0

        if self.current_squint_degree < 99.0:
            # EAR 값이 임계값(0.2)보다 작으면 찡그림으로 간주
            self.normalized_squint_degree = max(0.0, self.config["SQUINT_EAR_THRESHOLD"] - self.current_squint_degree)
        else:
            if "SQUINT_TIMER" in self.expression_start_time:
                self.normalized_squint_degree = 0.01
            else:
                self.normalized_squint_degree = 0.0

        if self.current_gaze_degree < 99.0:
            self.normalized_gaze_degree = self.current_gaze_degree
        else:
            self.normalized_gaze_degree = 0.0

        self.frame_counter += 1





    def _update_feedback_logic(self, frame_timestamp):
        current_time_sec = frame_timestamp / 1000.0

        is_frown = self.normalized_frown_degree > 0
        is_squint = self.normalized_squint_degree > 0

        is_squint_long_enough = False
        if is_squint:
            if "SQUINT_TIMER" not in self.expression_start_time:
                self.expression_start_time["SQUINT_TIMER"] = current_time_sec
            else:
                squint_duration = current_time_sec - self.expression_start_time["SQUINT_TIMER"]
                if squint_duration > self.config["SQUINT_DURATION_THRESHOLD"]:
                    is_squint_long_enough = True
        else:
            if "SQUINT_TIMER" in self.expression_start_time:
                del self.expression_start_time["SQUINT_TIMER"]

        is_gaze_averted = self.normalized_gaze_degree > self.config["GAZE_SIDE_THRESHOLD"]

        # 각각 독립적으로 타이머 관리
        if is_gaze_averted:
            if "GAZE_AVERTED" not in self.expression_start_time:
                self.expression_start_time["GAZE_AVERTED"] = current_time_sec
        else:
            if "GAZE_AVERTED" in self.expression_start_time:
                del self.expression_start_time["GAZE_AVERTED"]

        if is_frown:
            if "FROWN" not in self.expression_start_time:
                self.expression_start_time["FROWN"] = current_time_sec
        else:
            if "FROWN" in self.expression_start_time:
                del self.expression_start_time["FROWN"]

        if is_squint_long_enough:
            if "SQUINT" not in self.expression_start_time:
                self.expression_start_time["SQUINT"] = current_time_sec
        else:
            if "SQUINT" in self.expression_start_time:
                del self.expression_start_time["SQUINT"]

        feedback_was_set = False
        feedback_messages = []

        if "GAZE_AVERTED" in self.expression_start_time:
            duration = current_time_sec - self.expression_start_time["GAZE_AVERTED"]
            if (self.current_state == "LISTENING" and duration > self.config["LISTENING_GAZE_DURATION"]) or (
                    self.current_state == "SPEAKING" and duration > self.config["SPEAKING_GAZE_DURATION"]):
                feedback_messages.append("gaze")
                feedback_was_set = True

        if "FROWN" in self.expression_start_time:
            duration = current_time_sec - self.expression_start_time["FROWN"]
            if (self.current_state == "LISTENING" and duration > self.config["LISTENING_TENSE_DURATION"]) or (
                    self.current_state == "SPEAKING" and duration > self.config["SPEAKING_TENSE_DURATION"]):
                feedback_messages.append("frown")
                feedback_was_set = True

        if "SQUINT" in self.expression_start_time:
            duration = current_time_sec - self.expression_start_time["SQUINT"]
            if (self.current_state == "LISTENING" and duration > self.config["LISTENING_TENSE_DURATION"]) or (
                    self.current_state == "SPEAKING" and duration > self.config["SPEAKING_TENSE_DURATION"]):
                feedback_messages.append("squint")
                feedback_was_set = True

        if feedback_was_set:
            self.feedback_message = " | ".join(feedback_messages)
            self.expression_start_time["MESSAGE_DISPLAY"] = current_time_sec
        elif not is_frown and not is_squint_long_enough and not is_gaze_averted:
            self.feedback_message = ""
            if "MESSAGE_DISPLAY" in self.expression_start_time:
                del self.expression_start_time["MESSAGE_DISPLAY"]

        if self.last_known_bbox is None:
            self.feedback_message = "CANNOT detect face"
            self.expression_start_time = {}


if __name__ == '__main__':
    pass