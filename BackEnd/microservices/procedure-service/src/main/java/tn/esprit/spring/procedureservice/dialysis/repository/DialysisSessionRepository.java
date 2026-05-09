package tn.esprit.spring.procedureservice.dialysis.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisSession;

public interface DialysisSessionRepository extends JpaRepository<DialysisSession, Long> {
    boolean existsByPlanIdAndSessionDate(Long planId, LocalDateTime sessionDate);
    List<DialysisSession> findByPlanIdOrderBySessionDateAsc(Long planId);
    Optional<DialysisSession> findByIdempotencyKey(String idempotencyKey);
}

