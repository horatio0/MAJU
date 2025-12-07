import cv2
import time
import mediapipe as mp
from eye import FaceMeshAnalyzer, CONFIG, LANDMARK_SPECS, LEFT_EYE_INDICES, RIGHT_EYE_INDICES
from mouth import MouthAnalyzer
from pose import PoseAnalyzer


class IntegratedAnalysisSystem:
    def __init__(self, video_source=0, gender="male"):
        """통합 분석 시스템 초기화"""
        self.video_source = video_source
        self.is_webcam = isinstance(video_source, int)
        
        # Face Detection 초기화 (공유)
        self.mp_face_detection = mp.solutions.face_detection
        self.face_detector = self.mp_face_detection.FaceDetection(
            model_selection=1,
            min_detection_confidence=0.3
        )
        
        # 분석기 초기화
        self.eye_analyzer = FaceMeshAnalyzer(
            config=CONFIG,
            landmark_specs=LANDMARK_SPECS,
            left_eye_indices=LEFT_EYE_INDICES,
            right_eye_indices=RIGHT_EYE_INDICES
        )
        self.mouth_analyzer = MouthAnalyzer()
        self.pose_analyzer = PoseAnalyzer(gender=gender)
        
        # 분석 통계
        self.total_frames = 0
        self.analyzed_frames = 0
        self.start_time = None
        self.end_time = None
        
        # 문제 발생 횟수 카운터
        self.issue_count = {
            'eye': {
                'gaze_averted': 0,
                'frown': 0,
                'squint': 0,
                'face_not_detected': 0,
                'issues': 0
            },
            'mouth': {
                'jaw_forward': 0,
                'jaw_side': 0,
                'jaw_open': 0,
                'mouth_funnel': 0,
                'lip_pucker': 0,
                'smile': 0,
                'mouth_frown': 0,
                'mouth_dimple': 0,
                'mouth_stretch': 0,
                'lip_roll': 0,
                'chin_raise': 0,
                'lip_press': 0,
                'lower_lip_down': 0,
                'upper_lip_up': 0,
                'cheek_puff': 0,
                'cheek_squint': 0,
                'nose_sneer': 0,
                'issues': 0
            },
            'pose': {
                'shoulder': 0,
                'face_forward': 0,
                'body_tilt': 0,
                'knee_position': 0,
                'knee_balance': 0,
                'hand_position': 0,
                'stiff_posture': 0,
                'leg_alignment': 0,
                'pose_not_detected': 0,
                'issues': 0
            }
        }
        
        # 프레임별 세부 이슈 기록
        self.frame_details = []
    
    def close(self):
        """모든 분석기 리소스 해제"""
        self.face_detector.close()
        self.eye_analyzer.close()
        self.mouth_analyzer.close()
        self.pose_analyzer.close()
    
    
    def run(self):
        """통합 분석 실행"""
        cap = cv2.VideoCapture(self.video_source)
        
        if not cap.isOpened():
            return
        
        # 비디오 파일의 회전 메타데이터 자동 처리 활성화
        if not self.is_webcam:
            cap.set(cv2.CAP_PROP_ORIENTATION_AUTO, 1)
        
        # 비디오 설정
        orig_w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        orig_h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        
        if orig_h == 0 or orig_w == 0:
            return
        
        # 프레임 크기 설정 (해상도 조정)
        target_w = min(orig_w, 960)
        target_h = int(target_w * orig_h / orig_w)
        
        self.eye_analyzer.set_frame_size(target_w, target_h)
        
        # 분석 시작 시간 기록
        self.start_time = time.time()
        start_time_global = time.time()
        frame_counter = 0
        last_analysis_frame = -5  # 첫 프레임부터 분석하도록
        
        while cap.isOpened():
            success, frame = cap.read()
            if not success:
                if self.is_webcam:
                    continue
                else:
                    break
            
            frame_counter += 1
            self.total_frames += 1
            
            # 프레임 리사이징
            frame_resized = cv2.resize(frame, (target_w, target_h))
            
            # 타임스탬프 계산
            if self.is_webcam:
                timestamp_ms = int((time.time() - start_time_global) * 1000)
            else:
                timestamp_ms = int(cap.get(cv2.CAP_PROP_POS_MSEC))
            
            # === 5프레임마다만 분석 수행 ===
            if frame_counter - last_analysis_frame >= 5:
                last_analysis_frame = frame_counter
                self.analyzed_frames += 1
                
                # Face Detection (한 번만 실행)
                face_bbox = None
                rgb_frame = cv2.cvtColor(frame_resized, cv2.COLOR_BGR2RGB)
                rgb_frame.flags.writeable = False
                detect_result = self.face_detector.process(rgb_frame)
                rgb_frame.flags.writeable = True
                
                if detect_result.detections:
                    detection = detect_result.detections[0]
                    bbox_coordinates = detection.location_data.relative_bounding_box
                    
                    x1 = int(bbox_coordinates.xmin * target_w)
                    y1 = int(bbox_coordinates.ymin * target_h)
                    w = int(bbox_coordinates.width * target_w)
                    h = int(bbox_coordinates.height * target_h)
                    
                    padding_w = int(w * 0.2)
                    padding_h = int(h * 0.2)
                    
                    fx1 = max(0, x1 - padding_w)
                    fy1 = max(0, y1 - padding_h)
                    fx2 = min(target_w, (x1 + w) + padding_w)
                    fy2 = min(target_h, (y1 + h) + padding_h)
                    
                    face_bbox = (fx1, fy1, fx2, fy2)
                
                # 분석 수행 (face_bbox 전달)
                eye_result = self.eye_analyzer.process_frame(frame_resized, timestamp_ms, frame_counter, face_bbox)
                mouth_result = self.mouth_analyzer.process_frame(frame_resized, timestamp_ms, frame_counter, face_bbox)
                pose_result = self.pose_analyzer.process_frame(frame_resized, timestamp_ms, frame_counter)
                
                # 결과 카운터 업데이트
                for key, value in eye_result.items():
                    if value:
                        self.issue_count['eye'][key] += 1
                
                for key, value in mouth_result.items():
                    if value:
                        self.issue_count['mouth'][key] += 1
                
                for key, value in pose_result.items():
                    if value:
                        self.issue_count['pose'][key] += 1
                
                # 프레임별 이슈 기록 (이슈가 있는 프레임만)
                frame_issues = {
                    'eye': [k for k, v in eye_result.items() if v and k != 'issues'],
                    'mouth': [k for k, v in mouth_result.items() if v and k != 'issues'],
                    'pose': [k for k, v in pose_result.items() if v and k != 'issues']
                }
                
                # 이슈가 하나라도 있으면 기록
                if any(frame_issues.values()):
                    self.frame_details.append({
                        'frame_number': frame_counter,
                        'issues': frame_issues
                    })
        
        # 분석 종료 시간 기록
        self.end_time = time.time()
        
        cap.release()
    
    def get_analysis_results(self):
        """
        최종 분석 결과 반환 (FastAPI용)
        
        Returns:
            dict: 전체 분석 결과
        """
        # Eye 최종 점수
        eye_score = self.eye_analyzer.comprehensive_score
        
        # 분석 시간 계산
        analysis_duration = self.end_time - self.start_time if self.end_time and self.start_time else 0
        
        return {
            'metadata': {
                'total_frames': self.total_frames,
                'analyzed_frames': self.analyzed_frames,
                'duration_seconds': round(analysis_duration, 2),
                'video_source': str(self.video_source),
                'is_webcam': self.is_webcam
            },
            'issue_count': self.issue_count,
            'frame_details': self.frame_details,
            'scores': {
                'eye': {
                    'comprehensive_score': round(eye_score, 2)
                }
            }
        }


def main():
    """메인 실행 함수"""
    import sys
    
    # 명령줄 인자 파싱
    video_source = "phone.mp4"  # 기본: 비디오 파일
    gender = "male"
    
    if len(sys.argv) > 1:
        arg = sys.argv[1]
        if arg.isdigit():
            video_source = int(arg)
        else:
            video_source = arg
    
    if len(sys.argv) > 2:
        if sys.argv[2] in ["male", "female"]:
            gender = sys.argv[2]
    
    # 시스템 생성 및 실행
    system = IntegratedAnalysisSystem(video_source=video_source, gender=gender)
    
    try:
        system.run()
    except KeyboardInterrupt:
        pass
    finally:
        system.close()


if __name__ == '__main__':
    main()
