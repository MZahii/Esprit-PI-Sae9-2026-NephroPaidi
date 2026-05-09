package tn.esprit.spring.clinicalservice.discharge.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFollowUpItemRequest {
    private String itemType;
    private String description;
    private String frequency;
}
