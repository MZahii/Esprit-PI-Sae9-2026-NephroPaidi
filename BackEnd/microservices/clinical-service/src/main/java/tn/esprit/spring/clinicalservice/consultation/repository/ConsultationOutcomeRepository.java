package tn.esprit.spring.clinicalservice.consultation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationOutcome;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationOutcomeRepository extends JpaRepository<ConsultationOutcome, UUID> {
    Optional<ConsultationOutcome> findByConsultationId(UUID consultationId);

    List<ConsultationOutcome> findByConsultationIdIn(Collection<UUID> consultationIds);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT co FROM ConsultationOutcome co WHERE co.consultationId = :consultationId")
    Optional<ConsultationOutcome> findByConsultationIdForUpdate(@Param("consultationId") UUID consultationId);
}
