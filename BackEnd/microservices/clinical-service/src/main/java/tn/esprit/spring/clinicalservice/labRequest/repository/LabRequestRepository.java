package tn.esprit.spring.clinicalservice.labRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.spring.clinicalservice.labRequest.entity.LabRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LabRequestRepository extends JpaRepository<LabRequest, UUID> {
    List<LabRequest> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId);
    List<LabRequest> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<LabRequest> findByConsultationIdOrderByCreatedAtDesc(UUID consultationId);
    java.util.Optional<LabRequest> findTopByConsultationIdOrderByCreatedAtDesc(UUID consultationId);
    List<LabRequest> findByStatusOrderByCreatedAtDesc(LabRequest.LabStatus status);
    
    @Query("SELECT lr FROM LabRequest lr WHERE lr.status = :status ORDER BY lr.createdAt DESC")
    List<LabRequest> findPending(@Param("status") LabRequest.LabStatus status);

    @Query("SELECT lr FROM LabRequest lr WHERE lr.doctorId = :doctorId AND lr.status = :status ORDER BY lr.createdAt DESC")
    List<LabRequest> findByDoctorAndStatus(@Param("doctorId") UUID doctorId, @Param("status") LabRequest.LabStatus status);
}
