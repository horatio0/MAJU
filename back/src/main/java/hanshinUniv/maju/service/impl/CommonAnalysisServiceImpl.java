package hanshinUniv.maju.service.impl;

import hanshinUniv.maju.dto.commonanalysis.CommonAnalysisRequest;
import hanshinUniv.maju.dto.commonanalysis.CommonAnalysisResponse;
import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisRequest;
import hanshinUniv.maju.entity.CommonAnalysis;
import hanshinUniv.maju.entity.DetailAnalysis;
import hanshinUniv.maju.entity.Answer;
import hanshinUniv.maju.exception.ResourceNotFoundException;
import hanshinUniv.maju.mapper.CommonAnalysisMapper;
import hanshinUniv.maju.mapper.DetailAnalysisMapper;
import hanshinUniv.maju.repository.CommonAnalysisRepository;
import hanshinUniv.maju.repository.DetailAnalysisRepository;
import hanshinUniv.maju.service.CommonAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommonAnalysisServiceImpl implements CommonAnalysisService {
    private final CommonAnalysisRepository commonAnalysisRepository;
    private final CommonAnalysisMapper commonAnalysisMapper;

    @Autowired
    public CommonAnalysisServiceImpl(CommonAnalysisRepository commonAnalysisRepository,
                                     CommonAnalysisMapper commonAnalysisMapper) {
        this.commonAnalysisRepository = commonAnalysisRepository;
        this.commonAnalysisMapper = commonAnalysisMapper;
    }

    @Override
    @Transactional
    public CommonAnalysisResponse create(Long answerId, CommonAnalysisRequest request) {
        CommonAnalysis commonAnalysis = commonAnalysisMapper.requestToEntity(request);
        // answerId는 nullable이므로 null 허용

        CommonAnalysis saved = commonAnalysisRepository.save(commonAnalysis);

        return commonAnalysisMapper.entityToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CommonAnalysisResponse findById(Long id) {
        return commonAnalysisRepository.findById(id)
                .map(commonAnalysisMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("CommonAnalysis not found id=" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public CommonAnalysisResponse findByAnswerId(Long answerId) {
        return commonAnalysisRepository.findByAnswerId(answerId)
                .map(commonAnalysisMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("CommonAnalysis not found for answerId=" + answerId));
    }

    @Override
    @Transactional(readOnly = true)
    public CommonAnalysis findEntityByAnswerId(Long answerId) {
        return commonAnalysisRepository.findByAnswerId(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("CommonAnalysis not found for answerId=" + answerId));
    }

    @Override
    @Transactional(readOnly = true)
    public CommonAnalysis findEntityById(Long id) {
        return commonAnalysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommonAnalysis not found id=" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommonAnalysisResponse> findAll(Pageable pageable) {
        return commonAnalysisRepository.findAll(pageable).map(commonAnalysisMapper::entityToResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!commonAnalysisRepository.existsById(id)) throw new ResourceNotFoundException("CommonAnalysis not found id=" + id);
        commonAnalysisRepository.deleteById(id);
    }
}