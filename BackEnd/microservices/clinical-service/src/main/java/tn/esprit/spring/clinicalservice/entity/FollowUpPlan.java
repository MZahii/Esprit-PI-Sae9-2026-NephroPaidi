package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;

/**
 * Follow-up plan embeddable for discharge document.
 * Specifies outpatient follow-up schedule and objectives.
 */
@Embeddable
public class FollowUpPlan {
    
    @Column(name = "followup_general_practitioner", columnDefinition = "TEXT")
    private String generalPractitionerName;
    
    @Column(name = "followup_timeline", columnDefinition = "TEXT")
    private String followupTimeline;  // e.g., "1 week, 1 month, 3 months"
    
    @Column(name = "followup_objectives", columnDefinition = "TEXT")
    private String followupObjectives;  // e.g., "Monitor renal function, check blood pressure"
    
    @Column(name = "followup_specialist_referrals", columnDefinition = "TEXT")
    private String specialistReferrals;  // e.g., "Nephrology, Cardiology"
    
    @Column(name = "followup_additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;
    
    // Getters and Setters
    public String getGeneralPractitionerName() { return generalPractitionerName; }
    public void setGeneralPractitionerName(String generalPractitionerName) { this.generalPractitionerName = generalPractitionerName; }
    
    public String getFollowupTimeline() { return followupTimeline; }
    public void setFollowupTimeline(String followupTimeline) { this.followupTimeline = followupTimeline; }
    
    public String getFollowupObjectives() { return followupObjectives; }
    public void setFollowupObjectives(String followupObjectives) { this.followupObjectives = followupObjectives; }
    
    public String getSpecialistReferrals() { return specialistReferrals; }
    public void setSpecialistReferrals(String specialistReferrals) { this.specialistReferrals = specialistReferrals; }
    
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
}
