package tn.esprit.spring.clinicalservice.consultation.metrics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationMetricsRepository extends JpaRepository<ConsultationMetrics, UUID> {

    Optional<ConsultationMetrics> findByConsultationId(UUID consultationId);

    Optional<ConsultationMetrics> findTopByPatientIdAndConsultationIdNotOrderByCreatedAtDesc(Long patientId, UUID consultationId);
}
