from config.blendshape_config import BLENDSHAPE_CONFIG

# 임계값 설정
THRESHOLD_CONFIG = {blendshape_name: 0.4 for blendshape_name in BLENDSHAPE_CONFIG}
THRESHOLD_CONFIG.update({
    "jawForward": 0.4,
    "jawLeft":0.4,
    "jawRight":0.4,
    "mouthSmileLeft": 0.15,
    "mouthSmileRight": 0.15,
    "mouthPucker": 0.8,
    "cheekPuff":0.4,
    "cheekSquintLeft": 0.5,
    "mouthShrugLower": 0.5,
    "mouthShrugUpper": 0.5
})