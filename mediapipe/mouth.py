import cv2
import mediapipe as mp
from config.blendshape_config import BLENDSHAPE_CONFIG
from config.threshold_config import THRESHOLD_CONFIG

BaseOptions = mp.tasks.BaseOptions
VisionRunningMode = mp.tasks.vision.RunningMode

# faceLandmarker
FaceLandmarker = mp.tasks.vision.FaceLandmarker
FaceLandmarkerOptions = mp.tasks.vision.FaceLandmarkerOptions
FaceLandmarkerResult = mp.tasks.vision.FaceLandmarkerResult


class MouthAnalyzer:
    def __init__(self):
        """입 표정 분석기 초기화"""
        # faceLandmarker 초기화
        self.faceLandmarker = FaceLandmarker.create_from_options(FaceLandmarkerOptions(
            base_options=BaseOptions(model_asset_path="face_landmarker.task"),
            running_mode=VisionRunningMode.IMAGE,
            num_faces=1,
            output_face_blendshapes=True
        ))
    
    
    def close(self):
        """모델 리소스 해제"""
        self.faceLandmarker.close()
    
    
    def process_frame(self, frame, timestamp_ms, frame_number=None, face_bbox=None):
        """
        프레임 분석 수행 (단순 분석만)
        
        Args:
            frame: BGR 이미지 프레임
            timestamp_ms: 타임스탬프 (밀리초)
            frame_number: 외부에서 관리하는 프레임 번호 (옵션)
            face_bbox: main에서 전달받은 얼굴 bbox (fx1, fy1, fx2, fy2)
            
        Returns:
            dict: 분석 결과 데이터 (bool 딕셔너리)
        """
        # 초기화: 모든 키를 False로
        result_data = {
            'jaw_forward': False,      # AU29
            'jaw_side': False,         # AU30
            'jaw_open': False,         # AU26
            'mouth_funnel': False,     # AU22
            'lip_pucker': False,       # AU18
            'smile': False,            # AU12
            'mouth_frown': False,      # AU15
            'mouth_dimple': False,     # AU14
            'mouth_stretch': False,    # AU20
            'lip_roll': False,         # AU28
            'chin_raise': False,       # AU17
            'lip_press': False,        # AU24
            'lower_lip_down': False,   # AU16
            'upper_lip_up': False,     # AU10
            'cheek_puff': False,       # AU34
            'cheek_squint': False,     # AU6
            'nose_sneer': False        # AU9
        }
        
        if not face_bbox:
            return result_data
        
        fx1, fy1, fx2, fy2 = face_bbox
        
        # ROI 추출 및 분석
        face_roi = frame[fy1:fy2, fx1:fx2]
        
        if face_roi.size > 0:
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, 
                               data=cv2.cvtColor(face_roi, cv2.COLOR_BGR2RGB))
            
            face_landmarker_result = self.faceLandmarker.detect(mp_image)
            
            if face_landmarker_result and face_landmarker_result.face_blendshapes:
                blendshapes = face_landmarker_result.face_blendshapes[0]
                active_blendshapes = self._get_active_blendshape(blendshapes)
                
                # AU별로 그룹화하여 bool 설정
                au_groups = {}
                for blendshape_name, blendshape_score, au_name, description in active_blendshapes:
                    au_groups.setdefault(au_name, []).append(blendshape_name)
                
                # AU를 의미있는 키로 매핑
                au_to_key = {
                    'AU29': 'jaw_forward',
                    'AU30': 'jaw_side',
                    'AU26': 'jaw_open',
                    'AU22': 'mouth_funnel',
                    'AU18': 'lip_pucker',
                    'AU12': 'smile',
                    'AU15': 'mouth_frown',
                    'AU14': 'mouth_dimple',
                    'AU20': 'mouth_stretch',
                    'AU28': 'lip_roll',
                    'AU17': 'chin_raise',
                    'AU24': 'lip_press',
                    'AU16': 'lower_lip_down',
                    'AU10': 'upper_lip_up',
                    'AU34': 'cheek_puff',
                    'AU6': 'cheek_squint',
                    'AU9': 'nose_sneer'
                }
                
                for au_name in au_groups.keys():
                    key = au_to_key.get(au_name)
                    if key:
                        result_data[key] = True
        
        # 발생한 문제 목록 추가
        issues = [key for key, value in result_data.items() if value]
        result_data['issues'] = issues
        
        return result_data
    
    
    def _get_active_blendshape(self, blendshape_list):
        """
        mediapipe의 face_blendshape 결과 중 임계값을 넘은 항목만 골라 대응되는 AU 코드와 함께 반환.

        Args:
            blendshape_list (list):
                mediapipe FaceLandmarkerResult의 face_blendshape list.

        Returns:
            active_blendshape (list):
                활성화된 blendshape 정보를 담은 리스트.
                각 원소는 (blendshape_name, blendshape_score, au_name, description)의 튜플 형태.
        """
        active_blendshape = []
        for item in blendshape_list:
            blendshape_name = item.category_name
            blendshape_score = item.score
            threshold = THRESHOLD_CONFIG.get(blendshape_name, 1.0)
            if blendshape_name in BLENDSHAPE_CONFIG and blendshape_score >= threshold:
                au_name, description = BLENDSHAPE_CONFIG.get(blendshape_name)
                active_blendshape.append((blendshape_name, blendshape_score, au_name, description))
        return active_blendshape


if __name__ == '__main__':
    pass