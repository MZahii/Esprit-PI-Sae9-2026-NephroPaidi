package tn.esprit.spring.clinicalservice.appointment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.spring.clinicalservice.appointment.entity.Appointment;
import tn.esprit.spring.clinicalservice.appointment.entity.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("""
        SELECT a FROM Appointment a
        WHERE (:doctorId IS NULL OR a.doctorId = :doctorId)
          AND (:patientId IS NULL OR a.patientId = :patientId)
          AND (:status IS NULL OR a.status = :status)
          AND a.status <> 'ARCHIVED'
          AND (a.scheduledAt >= COALESCE(:fromTime, a.scheduledAt))
          AND (a.scheduledAt <= COALESCE(:toTime, a.scheduledAt))
        ORDER BY a.scheduledAt ASC
        """)
    List<Appointment> findFiltered(
            @Param("doctorId") UUID doctorId,
            @Param("patientId") Long patientId,
            @Param("status") AppointmentStatus status,
            @Param("fromTime") LocalDateTime from,
            @Param("toTime") LocalDateTime to
    );

    @Query(value = """
        SELECT EXISTS(
            SELECT 1 FROM appointment a
            WHERE (:doctorId IS NULL OR a.doctor_id = :doctorId)
              AND (:patientId IS NULL OR a.patient_id = :patientId)
              AND (:excludeId IS NULL OR a.id <> :excludeId)
              AND UPPER(a.status) IN ('SCHEDULED', 'CONFIRMED')
              AND a.scheduled_at < :endTime
              AND (a.scheduled_at + (COALESCE(a.duration_minutes, 30) || ' minutes')::interval) > :startTime
        )
        """, nativeQuery = true)
    boolean existsOverlapping(
            @Param("doctorId") UUID doctorId,
            @Param("patientId") Long patientId,
            @Param("excludeId") UUID excludeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.scheduledAt BETWEEN :start AND :end ORDER BY a.scheduledAt ASC")
    List<Appointment> findByDoctorIdAndScheduledAtBetween(
            @Param("doctorId") UUID doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.scheduledAt BETWEEN :start AND :end ORDER BY a.scheduledAt ASC")
    List<Appointment> findByStatusAndScheduledAtBetween(
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
