package tn.esprit.spring.clinicalservice.appointment.dto;

import lombok.*;
import tn.esprit.spring.clinicalservice.appointment.entity.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private UUID id;
    private Long patientId;
    private String patientName;
    private UUID doctorId;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String reason;
    private AppointmentStatus status;
    private String cancellationReason;
    private LocalDateTime archivedAt;
    private String archivedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
