package tn.esprit.spring.clinicalservice.followup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.esprit.spring.clinicalservice.followup.FollowUpOffsetUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorFollowUpCreateRequest {

    @NotNull
    @Min(1)
    private Integer offsetAmount;

    @NotNull
    private FollowUpOffsetUnit offsetUnit;

    private String notes;
}
