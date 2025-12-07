package hanshinUniv.maju.repository;

import hanshinUniv.maju.entity.DetailAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetailAnalysisRepository extends JpaRepository<DetailAnalysis, Long> {
    List<DetailAnalysis> findByCommonAnalysisId(Long commonAnalysisId);
    Page<DetailAnalysis> findByCommonAnalysisId(Long commonAnalysisId, Pageable pageable);
}