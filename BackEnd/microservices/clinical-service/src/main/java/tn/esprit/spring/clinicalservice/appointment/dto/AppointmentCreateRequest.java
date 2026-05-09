package tn.esprit.spring.clinicalservice.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentCreateRequest {

    @NotNull(message = "patientId is required")
    private Long patientId;

    @NotNull(message = "doctorId is required")
    private UUID doctorId;

    @NotNull(message = "scheduledAt is required")
    private LocalDateTime scheduledAt;

    @Min(value = 10, message = "durationMinutes must be at least 10")
    @Max(value = 180, message = "durationMinutes must be at most 180")
    private Integer durationMinutes;

    private String reason;
}
