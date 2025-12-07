package hanshinUniv.maju.repository;

import hanshinUniv.maju.entity.AnalysisTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, Long> {
    Optional<AnalysisTask> findByTaskId(String taskId);
}
