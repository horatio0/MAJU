package hanshinUniv.maju.repository;

import aj.org.objectweb.asm.commons.Remapper;
import hanshinUniv.maju.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByUserId(Long userId);
    Page<Session> findByUserId(Long userId, Pageable pageable);
    Page<Session> findByUserEmail(String userEmail, Pageable pageable);
    Page<Session> findBySessionTitleContainingIgnoreCase(String keyword, Pageable pageable);
}

