package tn.esprit.spring.clinicalservice.discharge.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDischargeFollowUpRequest {
    private Long patientId;
    private List<CreateFollowUpItemRequest> items;
}
