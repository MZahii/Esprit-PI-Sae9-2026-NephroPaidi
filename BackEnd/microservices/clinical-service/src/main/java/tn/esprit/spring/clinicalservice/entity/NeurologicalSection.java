package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import tn.esprit.spring.clinicalservice.enums.*;

/**
 * Neurological section for hospitalization.
 * Includes IVH grade, periventricular leukomalacia, seizures.
 */
@Embeddable
public class NeurologicalSection {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "neuro_ivh_grade")
    private IVHGrade intraventricularHemorrhage;
    
    @Column(name = "neuro_periventricular_leukomalacia")
    private Boolean periventricularLeukomalacia;
    
    @Column(name = "neuro_seizures")
    private Boolean seizures;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "neuro_coding_score")
    private NeurologyScore neurologyCodingScore;
    
    // Getters and Setters
    public IVHGrade getIntraventricularHemorrhage() { return intraventricularHemorrhage; }
    public void setIntraventricularHemorrhage(IVHGrade intraventricularHemorrhage) { this.intraventricularHemorrhage = intraventricularHemorrhage; }
    
    public Boolean getPeriventricularLeukomalacia() { return periventricularLeukomalacia; }
    public void setPeriventricularLeukomalacia(Boolean periventricularLeukomalacia) { this.periventricularLeukomalacia = periventricularLeukomalacia; }
    
    public Boolean getSeizures() { return seizures; }
    public void setSeizures(Boolean seizures) { this.seizures = seizures; }
    
    public NeurologyScore getNeurologyCodingScore() { return neurologyCodingScore; }
    public void setNeurologyCodingScore(NeurologyScore neurologyCodingScore) { this.neurologyCodingScore = neurologyCodingScore; }
}
