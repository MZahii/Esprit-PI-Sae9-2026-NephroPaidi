package tn.esprit.spring.clinicalservice.labRequest.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLabRequestRequest {
    private Long patientId;
    private java.util.UUID consultationId;
    private String testType;
    private String urgency;
    private String notes;
    private List<LabRequestTestItemDto> testItems;
}
