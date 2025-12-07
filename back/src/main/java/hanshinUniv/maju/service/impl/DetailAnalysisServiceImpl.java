package hanshinUniv.maju.service.impl;

import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisRequest;
import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisResponse;
import hanshinUniv.maju.entity.CommonAnalysis;
import hanshinUniv.maju.entity.DetailAnalysis;
import hanshinUniv.maju.exception.ResourceNotFoundException;
import hanshinUniv.maju.mapper.DetailAnalysisMapper;
import hanshinUniv.maju.repository.CommonAnalysisRepository;
import hanshinUniv.maju.repository.DetailAnalysisRepository;
import hanshinUniv.maju.service.DetailAnalysisService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DetailAnalysisServiceImpl implements DetailAnalysisService {
    private final DetailAnalysisRepository detailAnalysisRepository;
    private final CommonAnalysisRepository commonAnalysisRepository;
    private final DetailAnalysisMapper detailAnalysisMapper;

    public DetailAnalysisServiceImpl(DetailAnalysisRepository detailAnalysisRepository,
                                     CommonAnalysisRepository commonAnalysisRepository,
                                     DetailAnalysisMapper detailAnalysisMapper) {
        this.detailAnalysisRepository = detailAnalysisRepository;
        this.commonAnalysisRepository = commonAnalysisRepository;
        this.detailAnalysisMapper = detailAnalysisMapper;
    }

    @Override
    @Transactional
    public List<DetailAnalysisResponse> create(Long commonAnalysisId, List<DetailAnalysisRequest> request) {
        CommonAnalysis ca = commonAnalysisRepository.findById(commonAnalysisId)
                .orElseThrow(() -> new ResourceNotFoundException("CommonAnalysis not found id=" + commonAnalysisId));

        List<DetailAnalysis> details = request.stream()
                .map(d -> DetailAnalysis.builder()
                            .frame(d.getFrame())
                            .face(d.getFace())
                            .pose(d.getPose())
                            .commonAnalysis(ca)
                            .build()
                ).toList();

        List<DetailAnalysis> savedDetails = detailAnalysisRepository.saveAll(details);

        // 엔티티 → DTO 변환
        return savedDetails.stream()
                           .map(detailAnalysisMapper::entityToResponse)
                           .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DetailAnalysisResponse findById(Long id) {
        return detailAnalysisRepository.findById(id)
                .map(detailAnalysisMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("DetailAnalysis not found id=" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DetailAnalysisResponse> findByCommonAnalysisId(Long commonAnalysisId, Pageable pageable) {
        return detailAnalysisRepository.findByCommonAnalysisId(commonAnalysisId, pageable)
                .map(detailAnalysisMapper::entityToResponse);
    }

    @Override
    @Transactional
    public DetailAnalysisResponse update(Long id, DetailAnalysisRequest request) {
        DetailAnalysis existing = detailAnalysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DetailAnalysis not found id=" + id));
        DetailAnalysis updated = existing.toBuilder()
                .frame(request.getFrame())
                .face(request.getFace())
                .pose(request.getPose())
                .build();
        DetailAnalysis saved = detailAnalysisRepository.save(updated);
        return detailAnalysisMapper.entityToResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!detailAnalysisRepository.existsById(id)) throw new ResourceNotFoundException("DetailAnalysis not found id=" + id);
        detailAnalysisRepository.deleteById(id);
    }
}
