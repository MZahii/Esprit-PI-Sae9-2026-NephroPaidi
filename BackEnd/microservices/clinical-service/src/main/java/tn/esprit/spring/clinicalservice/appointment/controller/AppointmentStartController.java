package tn.esprit.spring.clinicalservice.appointment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.appointment.dto.AppointmentResponse;
import tn.esprit.spring.clinicalservice.appointment.entity.Appointment;
import tn.esprit.spring.clinicalservice.appointment.entity.AppointmentStatus;
import tn.esprit.spring.clinicalservice.appointment.repository.AppointmentRepository;
import tn.esprit.spring.clinicalservice.appointment.service.AppointmentStartService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/clinical/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentStartController {

    private final AppointmentStartService appointmentStartService;
    private final AppointmentRepository appointmentRepository;

    @PostMapping("/{appointmentId}/start")
    public ResponseEntity<AppointmentResponse> startConsultation(@PathVariable UUID appointmentId) {
        log.info("Request to start consultation for appointment: {}", appointmentId);

        try {
            AppointmentResponse response = appointmentStartService.startConsultation(appointmentId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("Cannot start consultation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            log.warn("Appointment not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{appointmentId}/receptionist-override")
    public ResponseEntity<AppointmentResponse> receptionistOverride(
            @PathVariable UUID appointmentId,
            @RequestParam(defaultValue = "10") int extensionMinutes) {
        log.info("Receptionist override request for appointment: {} with extension: {} minutes", 
            appointmentId, extensionMinutes);

        try {
            AppointmentResponse response = appointmentStartService.receptionistOverrideWindow(appointmentId, extensionMinutes);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("Cannot override appointment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            log.warn("Appointment not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{appointmentId}/start-window-status")
    public ResponseEntity<Map<String, Object>> getStartWindowStatus(@PathVariable UUID appointmentId) {
        log.info("Checking start window status for appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElse(null);

        Map<String, Object> status = new HashMap<>();

        if (appointment == null) {
            status.put("available", false);
            status.put("message", "Appointment not found");
            return ResponseEntity.ok(status);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = appointment.getScheduledAt();
        LocalDateTime bufferStart = appointmentTime.minusMinutes(5);
        LocalDateTime hardCancelTime = appointmentTime.plusMinutes(30);

        // Check if today
        boolean isToday = appointmentTime.toLocalDate().equals(now.toLocalDate());
        
        if (!isToday) {
            status.put("available", false);
            status.put("message", "Appointment is not today");
            status.put("scheduledFor", appointmentTime);
            return ResponseEntity.ok(status);
        }

        // Check appointment status
        if (!appointment.getStatus().equals(AppointmentStatus.SCHEDULED)) {
            status.put("available", false);
            status.put("message", "Appointment status is: " + appointment.getStatus());
            status.put("currentStatus", appointment.getStatus().toString());
            return ResponseEntity.ok(status);
        }

        // Button logic
        if (now.isBefore(bufferStart)) {
            status.put("available", false);
            status.put("message", "Too early. Button available at " + bufferStart);
            status.put("minutesUntilAvailable", 
                java.time.temporal.ChronoUnit.MINUTES.between(now, bufferStart));
            status.put("windowStart", bufferStart);
        } else if (now.isAfter(hardCancelTime)) {
            status.put("available", false);
            status.put("message", "Appointment window closed");
            status.put("closedAt", hardCancelTime);
        } else {
            status.put("available", true);
            status.put("message", "Button available - Start consultation now");
            status.put("minutesUntilClose",
                java.time.temporal.ChronoUnit.MINUTES.between(now, hardCancelTime));
            status.put("windowEnd", hardCancelTime);
        }

        return ResponseEntity.ok(status);
    }
}
