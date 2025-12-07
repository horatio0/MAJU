package hanshinUniv.maju.service;

import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisRequest;
import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DetailAnalysisService {
    List<DetailAnalysisResponse> create(Long commonAnalysisId, List<DetailAnalysisRequest> request);
    DetailAnalysisResponse findById(Long id);
    Page<DetailAnalysisResponse> findByCommonAnalysisId(Long commonAnalysisId, Pageable pageable);
    DetailAnalysisResponse update(Long id, DetailAnalysisRequest request);
    void delete(Long id);
}

