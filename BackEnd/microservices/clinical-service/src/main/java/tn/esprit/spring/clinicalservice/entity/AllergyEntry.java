package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import tn.esprit.spring.clinicalservice.enums.AllergyType;
import tn.esprit.spring.clinicalservice.enums.AllergySeverity;
import tn.esprit.spring.clinicalservice.enums.AllergyStatus;

/**
 * Allergy entry embeddable for element collection in ConsultationRecord.
 */
@Embeddable
public class AllergyEntry {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "allergy_type", nullable = false)
    private AllergyType allergyType;
    
    @Column(name = "responsible_agent", nullable = false)
    private String responsibleAgent;  // e.g., "Penicillin", "Shellfish"
    
    @Column(name = "reaction_type")
    private String reactionType;  // e.g., "Rash", "Anaphylaxis"
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private AllergySeverity severity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AllergyStatus status;
    
    // Getters and Setters
    public AllergyType getAllergyType() { return allergyType; }
    public void setAllergyType(AllergyType allergyType) { this.allergyType = allergyType; }
    
    public String getResponsibleAgent() { return responsibleAgent; }
    public void setResponsibleAgent(String responsibleAgent) { this.responsibleAgent = responsibleAgent; }
    
    public String getReactionType() { return reactionType; }
    public void setReactionType(String reactionType) { this.reactionType = reactionType; }
    
    public AllergySeverity getSeverity() { return severity; }
    public void setSeverity(AllergySeverity severity) { this.severity = severity; }
    
    public AllergyStatus getStatus() { return status; }
    public void setStatus(AllergyStatus status) { this.status = status; }
}
