package hanshinUniv.maju.repository;

import hanshinUniv.maju.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByCategory(String category);
    Page<Question> findByCategory(String category, Pageable pageable);
    Page<Question> findByQuestionContainingIgnoreCase(String keyword, Pageable pageable);
}

