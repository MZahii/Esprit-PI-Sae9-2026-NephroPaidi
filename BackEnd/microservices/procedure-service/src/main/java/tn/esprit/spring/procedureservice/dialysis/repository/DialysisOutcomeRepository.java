package tn.esprit.spring.procedureservice.dialysis.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisOutcome;

public interface DialysisOutcomeRepository extends JpaRepository<DialysisOutcome, Long> {
    boolean existsBySessionId(Long sessionId);
    Optional<DialysisOutcome> findBySessionId(Long sessionId);
}
