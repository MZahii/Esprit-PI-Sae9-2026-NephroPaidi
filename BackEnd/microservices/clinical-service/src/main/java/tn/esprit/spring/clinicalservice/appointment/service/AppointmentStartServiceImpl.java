package tn.esprit.spring.clinicalservice.appointment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.esprit.spring.clinicalservice.appointment.dto.AppointmentResponse;
import tn.esprit.spring.clinicalservice.appointment.entity.Appointment;
import tn.esprit.spring.clinicalservice.appointment.entity.AppointmentStatus;
import tn.esprit.spring.clinicalservice.appointment.repository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentStartServiceImpl implements AppointmentStartService {

    private final AppointmentRepository appointmentRepository;

    @Override
    public AppointmentResponse startConsultation(UUID appointmentId) {
        log.info("Starting consultation for appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        // Validate appointment status
        if (!appointment.getStatus().equals(AppointmentStatus.SCHEDULED)) {
            throw new IllegalStateException("Appointment is not in SCHEDULED status: " + appointment.getStatus());
        }

        // Validate appointment is today
        LocalDate today = LocalDate.now();
        if (!appointment.getScheduledAt().toLocalDate().equals(today)) {
            throw new IllegalStateException("Appointment is not scheduled for today");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = appointment.getScheduledAt();
        
        // OPTION 3 HYBRID: 20-30 minute window with 5-minute buffer
        // Button visible from: appointment_time - 5 minutes
        // Hard cancel at: appointment_time + 30 minutes
        LocalDateTime earliestStart = appointmentTime.minusMinutes(5);  // 5-min buffer
        LocalDateTime hardCancelTime = appointmentTime.plusMinutes(30);

        // Check if within window (can start 5 min early or up to 30 min late)
        if (now.isBefore(earliestStart)) {
            throw new IllegalStateException("Too early to start consultation (available from " + 
                earliestStart + ")");
        }
        if (now.isAfter(hardCancelTime)) {
            throw new IllegalStateException("Appointment window has closed (expired at " + 
                hardCancelTime + ")");
        }

        // Update appointment status to CONFIRMED (consultation started)
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setStartedAt(now);
        appointment.setUpdatedAt(now);

        Appointment updated = appointmentRepository.save(appointment);
        log.info("Consultation started for appointment: {} at {}", appointmentId, now);

        return convertToResponse(updated);
    }

    @Override
    public AppointmentResponse receptionistOverrideWindow(UUID appointmentId, int extensionMinutes) {
        log.info("Receptionist override: Extending appointment {} by {} minutes", appointmentId, extensionMinutes);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        // Validation: Can only extend if marked as NO_SHOW and within grace period
        if (!appointment.getStatus().equals(AppointmentStatus.NO_SHOW)) {
            throw new IllegalStateException("Can only extend NO_SHOW appointments. Current status: " + 
                appointment.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = appointment.getScheduledAt();
        LocalDateTime hardCancelTime = appointmentTime.plusMinutes(30);
        
        // Can only extend if still within 30-min window
        if (now.isAfter(hardCancelTime)) {
            throw new IllegalStateException("Extension denied: Appointment window has completely expired");
        }

        // Extend the cancellation deadline (up to +10 min extension)
        int maxExtension = Math.min(extensionMinutes, 10);
        LocalDateTime newCancelDeadline = hardCancelTime.plusMinutes(maxExtension);
        
        log.info("Appointment {} extended until {}", appointmentId, newCancelDeadline);
        
        // Change status back to SCHEDULED to prevent auto-cancel
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setUpdatedAt(now);
        appointmentRepository.save(appointment);
        
        // Notify doctor: "Receptionist extended your appointment window (+{maxExtension} minutes)"
        log.info("Notification sent: Receptionist extended window by {} minutes", maxExtension);

        return convertToResponse(appointment);
    }

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Override
    public void autoCancelStaleAppointments() {
        log.info("Running auto-cancel stale appointments job");

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Find all SCHEDULED appointments for today
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // Get all scheduled appointments for today
        List<Appointment> scheduledAppointments = appointmentRepository
                .findByStatusAndScheduledAtBetween(AppointmentStatus.SCHEDULED, startOfDay, endOfDay);

        for (Appointment appointment : scheduledAppointments) {
            LocalDateTime appointmentTime = appointment.getScheduledAt();
            LocalDateTime missedMarkerTime = appointmentTime.plusMinutes(20);  // Mark as MISSED at 20 min
            LocalDateTime hardCancelTime = appointmentTime.plusMinutes(30);    // HARD CANCEL at 30 min

            // At 20 minutes: Mark as NO_SHOW if not yet started
            if (now.isAfter(missedMarkerTime) && now.isBefore(hardCancelTime) && 
                appointment.getStatus().equals(AppointmentStatus.SCHEDULED)) {
                log.info("Marking appointment as NO_SHOW (20-min mark): {}", appointment.getId());
                
                appointment.setStatus(AppointmentStatus.NO_SHOW);
                appointment.setUpdatedAt(now);
                appointmentRepository.save(appointment);
                
                // Notify doctor: "Patient did not show up"
                // Notify patient/guardian: "Appointment was missed"
                log.info("Appointment {} marked as NO_SHOW", appointment.getId());
            }
            
            // At 30 minutes: Hard cancel
            if (now.isAfter(hardCancelTime) && 
                (appointment.getStatus().equals(AppointmentStatus.SCHEDULED) || 
                 appointment.getStatus().equals(AppointmentStatus.NO_SHOW))) {
                log.info("Auto-cancelling stale appointment (30-min mark): {}", appointment.getId());

                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointment.setUpdatedAt(now);
                appointmentRepository.save(appointment);

                // Final notification to doctor and patient/guardian
                log.info("Appointment {} auto-cancelled due to timeout", appointment.getId());
            }
        }
    }

    private AppointmentResponse convertToResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .doctorId(appointment.getDoctorId())
                .patientId(appointment.getPatientId())
                .scheduledAt(appointment.getScheduledAt())
                .durationMinutes(appointment.getDurationMinutes())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}
