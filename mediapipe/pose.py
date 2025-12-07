import cv2
import mediapipe as mp
import math

mp_holistic = mp.solutions.holistic

class PoseAnalyzer:
    def __init__(self, gender="male"):
        """자세 분석기 초기화"""
        self.gender = gender
        self.holistic = mp_holistic.Holistic(
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5,
            model_complexity=1
        )
    
    def close(self):
        """모델 리소스 해제"""
        self.holistic.close()

    def calculate_angle(self, p1, p2):
        """두 점 사이의 각도 계산 (수평 기준 0도)"""
        if p1 is None or p2 is None:
            return 0
        
        dx = p2[0] - p1[0]
        dy = p2[1] - p1[1]
        
        angle_rad = math.atan2(dy, dx)
        angle_deg = math.degrees(angle_rad)
        
        return angle_deg

    def process_frame(self, frame, timestamp_ms, frame_number=None):
        """
        프레임 분석 수행
        """
        result_data = {
            'shoulder': False,
            'face_forward': False,
            'body_tilt': False,
            'knee_position': False,
            'knee_balance': False,
            'hand_position': False,
            'stiff_posture': False,
            'leg_alignment': False,
            'pose_not_detected': False 
        }
        
        image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        image.flags.writeable = False
        results = self.holistic.process(image)
        
        # 1. 랜드마크 감지 확인
        if not results.pose_landmarks:
            result_data['pose_not_detected'] = True
            result_data['issues'] = ['pose_not_detected']
            return result_data

        landmarks = results.pose_landmarks.landmark
        
        # 2. 좌표 추출 헬퍼 (visibility 체크)
        def get_lm(idx):
            lm = landmarks[idx]
            return (lm.x, lm.y) if lm.visibility > 0.5 else None

        points = {
            'nose': get_lm(0),
            'l_sh': get_lm(11), 'r_sh': get_lm(12),   # 어깨
            'l_hip': get_lm(23), 'r_hip': get_lm(24), # 골반
            'l_knee': get_lm(25), 'r_knee': get_lm(26), # 무릎
            'l_ankle': get_lm(27), 'r_ankle': get_lm(28), # 발목 (추가됨)
            'l_wrist': get_lm(15), 'r_wrist': get_lm(16) # 손목
        }

        # 3. 전신 필수 조건 체크 (발목은 안보일 수도 있으므로 제외하고 무릎까지만 필수)
        required_body_parts = [
            points['l_sh'], points['r_sh'],
            points['l_hip'], points['r_hip'],
            points['l_knee'], points['r_knee']
        ]

        if any(pt is None for pt in required_body_parts):
            result_data['pose_not_detected'] = True
            result_data['issues'] = ['pose_not_detected']
            return result_data

        # 4. 평가 실행
        try:
            result_data = self._assess_geometry(points, self.gender)
            # 발생한 문제 목록 추가
            issues = [key for key, value in result_data.items() if value]
            result_data['issues'] = issues
        except Exception as e:
            # 디버깅을 위해 에러 메시지 출력 가능
            # print(f"Analysis Error: {e}")
            result_data['pose_not_detected'] = True
            result_data['issues'] = ['pose_not_detected']
        
        return result_data

    def _assess_geometry(self, p, gender):
        """기하학적(각도/비율) 자세 평가 로직"""
        result_data = {
            'shoulder': False,
            'face_forward': False,
            'body_tilt': False,
            'knee_position': False,
            'knee_balance': False,
            'hand_position': False,
            'stiff_posture': False,
            'leg_alignment': False
        }
        
        # 공통 변수 계산
        shoulder_width = abs(p['l_sh'][0] - p['r_sh'][0])
        shoulder_avg_y = (p['l_sh'][1] + p['r_sh'][1]) / 2
        
        # 어깨 기울기
        shoulder_angle = self.calculate_angle(p['l_sh'], p['r_sh'])
        if abs(180 - abs(shoulder_angle)) > 4.0:
            result_data["shoulder"] = True

        # 고개 돌림
        if p['nose']:
            shoulder_center_x = (p['l_sh'][0] + p['r_sh'][0]) / 2
            if shoulder_width > 0:
                deviation = abs(p['nose'][0] - shoulder_center_x) / shoulder_width
                if deviation > 0.15:
                    result_data["face_forward"] = True

        # 몸 기울기
        shoulder_center_x = (p['l_sh'][0] + p['r_sh'][0]) / 2
        hip_center_x = (p['l_hip'][0] + p['r_hip'][0]) / 2
        
        tilt_ratio = abs(shoulder_center_x - hip_center_x) / shoulder_width
        if tilt_ratio > 0.1:
            result_data["body_tilt"] = True

        # 무릎 벌림
        l_knee_x, r_knee_x = p['l_knee'][0], p['r_knee'][0]
        l_hip_x, r_hip_x = p['l_hip'][0], p['r_hip'][0]
        
        knee_dist = abs(l_knee_x - r_knee_x)
        hip_dist = abs(l_hip_x - r_hip_x)
        
        if gender == "male":
            if knee_dist > hip_dist * 2.5:
                result_data["knee_position"] = True
        elif gender == "female":
            if knee_dist > hip_dist * 1.2:
                result_data["knee_position"] = True

        # 무릎 수평 균형
        knee_angle = self.calculate_angle(p['l_knee'], p['r_knee'])
        if abs(180 - abs(knee_angle)) > 10.0:
            result_data["knee_balance"] = True

        # 손 위치
        # 손목이 어깨 높이보다 위로 올라가거나(얼굴 만짐), 
        # 가슴 높이 위에서 너무 산만하게 움직이는 경우를 감지
        if p['l_wrist'] and p['r_wrist']:
            if p['l_wrist'][1] < shoulder_avg_y or p['r_wrist'][1] < shoulder_avg_y:
                result_data["hand_position"] = True
            
            center_x = (p['l_hip'][0] + p['r_hip'][0]) / 2
            if (abs(p['l_wrist'][0] - center_x) > shoulder_width * 1.5 or 
                abs(p['r_wrist'][0] - center_x) > shoulder_width * 1.5):
                result_data["hand_position"] = True

        # 경직된 자세
        # 어깨가 귀(코) 쪽으로 바짝 올라간 상태 감지
        # 목의 길이(코~어깨중심)가 어깨 너비에 비해 비정상적으로 짧아지면 긴장 상태로 간주
        if p['nose']:
            nose_y = p['nose'][1]
            neck_length = abs(shoulder_avg_y - nose_y)
            
            if shoulder_width > 0:
                ratio = neck_length / shoulder_width
                if ratio < 0.25: 
                    result_data["stiff_posture"] = True

        # 8. 다리 정렬
        # 발목이 보일 경우, 발목 교차(Crossed Legs) 여부 확인
        if p['l_ankle'] and p['r_ankle']:
            l_ankle_x = p['l_ankle'][0]
            r_ankle_x = p['r_ankle'][0]
            
            # 발목 간의 수평 거리가 너무 가까우면(교차) 감점
            ankle_dist = abs(l_ankle_x - r_ankle_x)
            
            # 무릎보다 발목이 현저히 더 좁게 모여있거나 교차된 경우
            # (무릎 거리의 20% 미만으로 발목이 붙어있으면 꼬았다고 판단)
            if knee_dist > 0 and ankle_dist < knee_dist * 0.2:
                result_data["leg_alignment"] = True

            # 무릎보다 발목이 너무 널게 벌어진 경우
            if knee_dist > 0 and ankle_dist > knee_dist * 1.2:
                result_data["leg_alignment"] = True
                
            # 혹은 발목의 Y 위치가 무릎보다 현저히 높으면(다리를 들어올림) 감점
            # (앉은 자세 기준: 발목 Y > 무릎 Y 여야 함, 화면상 아래쪽이 큰 값)
            avg_knee_y = (p['l_knee'][1] + p['r_knee'][1]) / 2
            if p['l_ankle'][1] < avg_knee_y: # 발목이 무릎보다 위에 있음
                 result_data["leg_alignment"] = True

        return result_data