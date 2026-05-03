package tn.esprit.spring.clinicalservice.followup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.clinicalservice.followup.DoctorFollowUpStatus;
import tn.esprit.spring.clinicalservice.followup.entity.DoctorFollowUpRequest;

import java.util.List;
import java.util.UUID;

public interface DoctorFollowUpRequestRepository extends JpaRepository<DoctorFollowUpRequest, UUID> {

    List<DoctorFollowUpRequest> findByStatusOrderByCreatedAtDesc(DoctorFollowUpStatus status);

    List<DoctorFollowUpRequest> findByConsultationIdOrderByCreatedAtDesc(UUID consultationId);
}
