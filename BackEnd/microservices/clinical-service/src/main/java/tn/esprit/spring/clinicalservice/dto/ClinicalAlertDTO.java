package tn.esprit.spring.clinicalservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for ClinicalAlert REST endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalAlertDTO {
    private UUID id;
    private UUID patientId;
    private String alertType;
    private String severity;
    private String message;
    private String details;
    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;
    private Boolean resolved;
    private LocalDateTime resolvedAt;
}
