package tn.esprit.spring.clinicalservice.consultation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationCreateRequest {

    @NotNull(message = "patientId is required")
    private Long patientId;

    @NotNull(message = "dateTime is required")
    private LocalDateTime dateTime;

    // Optional reference to appointment (for referential tracking)
    private UUID appointmentId;

    // Used only in local profile for easier testing
    private UUID doctorId;
}
