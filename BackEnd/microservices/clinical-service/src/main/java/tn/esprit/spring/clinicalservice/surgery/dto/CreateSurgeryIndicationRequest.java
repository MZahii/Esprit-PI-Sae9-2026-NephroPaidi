package tn.esprit.spring.clinicalservice.surgery.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSurgeryIndicationRequest {
    private Long patientId;
    private String urgency;
    private String notes;
}
