package tn.esprit.spring.clinicalservice.dto;

import lombok.*;

/**
 * DTO for FollowUpPlan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpPlanDTO {
    private String generalPractitionerName;
    private String followupTimeline;
    private String followupObjectives;
    private String specialistReferrals;
    private String additionalNotes;
}
