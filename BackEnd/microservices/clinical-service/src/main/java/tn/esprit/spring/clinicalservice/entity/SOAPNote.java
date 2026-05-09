package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * SOAP note (Subjective, Objective, Assessment, Plan) embeddable.
 */
@Embeddable
public class SOAPNote {
    
    @Column(name = "subjective_soap", columnDefinition = "TEXT")
    private String subjectiveSOAP;
    
    @Column(name = "objective_soap", columnDefinition = "TEXT")
    private String objectiveSOAP;
    
    @Column(name = "assessment_soap", columnDefinition = "TEXT")
    private String assessmentSOAP;
    
    @Column(name = "plan_soap", columnDefinition = "TEXT")
    private String planSOAP;
    
    // Getters and Setters
    public String getSubjectiveSOAP() { return subjectiveSOAP; }
    public void setSubjectiveSOAP(String subjectiveSOAP) { this.subjectiveSOAP = subjectiveSOAP; }
    
    public String getObjectiveSOAP() { return objectiveSOAP; }
    public void setObjectiveSOAP(String objectiveSOAP) { this.objectiveSOAP = objectiveSOAP; }
    
    public String getAssessmentSOAP() { return assessmentSOAP; }
    public void setAssessmentSOAP(String assessmentSOAP) { this.assessmentSOAP = assessmentSOAP; }
    
    public String getPlanSOAP() { return planSOAP; }
    public void setPlanSOAP(String planSOAP) { this.planSOAP = planSOAP; }
}
