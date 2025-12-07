package hanshinUniv.maju.repository;

import hanshinUniv.maju.entity.CommonAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommonAnalysisRepository extends JpaRepository<CommonAnalysis, Long> {
    Optional<CommonAnalysis> findByAnswerId(Long answerId);
}