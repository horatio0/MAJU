package hanshinUniv.maju.service;

import hanshinUniv.maju.dto.commonanalysis.CommonAnalysisRequest;
import hanshinUniv.maju.dto.commonanalysis.CommonAnalysisResponse;
import hanshinUniv.maju.entity.CommonAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommonAnalysisService{
    CommonAnalysisResponse create(Long answerId, CommonAnalysisRequest request);
    CommonAnalysisResponse findById(Long id);
    CommonAnalysisResponse findByAnswerId(Long answerId);
    CommonAnalysis findEntityByAnswerId(Long answerId);
    CommonAnalysis findEntityById(Long id);
    Page<CommonAnalysisResponse> findAll(Pageable pageable);
    void delete(Long id);
}
