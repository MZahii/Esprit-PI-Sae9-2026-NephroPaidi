package tn.esprit.spring.clinicalservice.consultation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    List<Consultation> findByDoctorId(UUID doctorId);

    List<Consultation> findByDoctorIdAndPatientId(UUID doctorId, Long patientId);

    List<Consultation> findByDoctorIdAndPatientIdIn(UUID doctorId, List<Long> patientIds);

    List<Consultation> findByDoctorIdAndStatus(UUID doctorId, ConsultationStatus status);

    List<Consultation> findByDoctorIdAndPatientIdInAndStatus(UUID doctorId, List<Long> patientIds, ConsultationStatus status);

    List<Consultation> findByPatientIdIn(List<Long> patientIds);

    List<Consultation> findByPatientIdInAndStatus(List<Long> patientIds, ConsultationStatus status);

    Optional<Consultation> findByAppointmentId(UUID appointmentId);

    // Backoffice queries for listing all consultations with filters
    List<Consultation> findByPatientId(Long patientId);

    List<Consultation> findByStatus(ConsultationStatus status);

    List<Consultation> findByDateTimeBetween(LocalDateTime from, LocalDateTime to);

    List<Consultation> findByPatientIdAndStatus(Long patientId, ConsultationStatus status);

    List<Consultation> findByPatientIdAndDateTimeBetween(Long patientId, LocalDateTime from, LocalDateTime to);

    List<Consultation> findByStatusAndDateTimeBetween(ConsultationStatus status, LocalDateTime from, LocalDateTime to);

    List<Consultation> findByPatientIdAndStatusAndDateTimeBetween(Long patientId, ConsultationStatus status, LocalDateTime from, LocalDateTime to);
}
