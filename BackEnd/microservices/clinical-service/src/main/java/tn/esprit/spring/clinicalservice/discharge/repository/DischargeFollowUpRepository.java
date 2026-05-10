package tn.esprit.spring.clinicalservice.discharge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.clinicalservice.discharge.entity.DischargeFollowUp;

import java.util.List;
import java.util.UUID;

public interface DischargeFollowUpRepository extends JpaRepository<DischargeFollowUp, UUID> {
    List<DischargeFollowUp> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<DischargeFollowUp> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId);
    List<DischargeFollowUp> findByStatusOrderByCreatedAtDesc(DischargeFollowUp.FollowUpStatus status);
}
