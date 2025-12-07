from fastapi import FastAPI, File, UploadFile, Form
from fastapi.responses import JSONResponse
import os
import tempfile
import traceback
from main import IntegratedAnalysisSystem


app = FastAPI(title="면접 분석 시스템 API")


@app.post("/analyze")
async def analyze_video(file: UploadFile = File(...), gender: str = Form("male")):
    """
    비디오 파일 업로드 후 면접 분석 수행
    
    Args:
        file: 비디오 파일 (mp4, avi, mov 등)
        gender: 성별 (male/female)
    
    Returns:
        분석 결과 JSON
    """
    # 임시 파일 저장
    with tempfile.NamedTemporaryFile(delete=False, suffix='.mp4') as temp:
        content = await file.read()
        temp.write(content)
        tmp_path = temp.name

    try:
        # 분석 실행
        system = IntegratedAnalysisSystem(video_source=tmp_path, gender=gender)
        system.run()
        results = system.get_analysis_results()
        system.close()

        return JSONResponse(content=results)
    except Exception as e:
        error_detail = {
            "error": str(e),
            "type": type(e).__name__,
            "traceback": traceback.format_exc()
        }
        return JSONResponse(
            status_code=500,
            content=error_detail
        )
    finally:
        # 임시 파일 삭제
        if os.path.exists(tmp_path):
            os.unlink(tmp_path)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)
