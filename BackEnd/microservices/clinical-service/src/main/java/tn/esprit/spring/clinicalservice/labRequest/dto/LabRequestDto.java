package tn.esprit.spring.clinicalservice.labRequest.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabRequestDto {
    private UUID id;
    private UUID doctorId;
    private Long patientId;
    private String testType;
    private String urgency;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
