package tn.esprit.spring.clinicalservice.surgery.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurgeryIndicationDto {
    private UUID id;
    private UUID doctorId;
    private Long patientId;
    private String urgency;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
}
