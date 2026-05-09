package tn.esprit.spring.clinicalservice.surgery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.clinicalservice.surgery.entity.SurgeryIndication;

import java.util.List;
import java.util.UUID;

public interface SurgeryIndicationRepository extends JpaRepository<SurgeryIndication, UUID> {
    List<SurgeryIndication> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId);
    List<SurgeryIndication> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<SurgeryIndication> findByStatusOrderByCreatedAtDesc(SurgeryIndication.SurgeryStatus status);
}
