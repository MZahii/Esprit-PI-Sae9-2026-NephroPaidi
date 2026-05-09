package tn.esprit.spring.clinicalservice.appointment.dto;

import lombok.*;
import tn.esprit.spring.clinicalservice.appointment.entity.AppointmentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentUpdateRequest {

    private Long patientId;

    private UUID doctorId;

    private LocalDateTime scheduledAt;

    @Min(value = 10, message = "durationMinutes must be at least 10")
    @Max(value = 180, message = "durationMinutes must be at most 180")
    private Integer durationMinutes;

    private String reason;

    private AppointmentStatus status;
}
