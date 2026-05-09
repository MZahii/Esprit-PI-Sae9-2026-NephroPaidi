package tn.esprit.spring.clinicalservice.consultation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationUpdateRequest {

    private LocalDateTime dateTime;

    @NotNull(message = "status is required")
    private ConsultationStatus status;
}