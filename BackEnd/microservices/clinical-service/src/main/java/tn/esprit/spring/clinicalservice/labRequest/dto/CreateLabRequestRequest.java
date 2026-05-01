package tn.esprit.spring.clinicalservice.labRequest.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLabRequestRequest {
    private Long patientId;
    private String testType;
    private String urgency;
    private String notes;
}
