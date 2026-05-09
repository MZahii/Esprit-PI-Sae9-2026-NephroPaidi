package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import tn.esprit.spring.clinicalservice.enums.*;

/**
 * Cardiac pathology section for hospitalization.
 * Gated by hasCardiacPathology boolean.
 */
@Embeddable
public class CardiacSection {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "card_pathology_type")
    private CardiacPathologyType cardiacPathologyType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "card_pda_treatment")
    private PDA_Treatment pdaTreatment;
    
    // Getters and Setters
    public CardiacPathologyType getCardiacPathologyType() { return cardiacPathologyType; }
    public void setCardiacPathologyType(CardiacPathologyType cardiacPathologyType) { this.cardiacPathologyType = cardiacPathologyType; }
    
    public PDA_Treatment getPdaTreatment() { return pdaTreatment; }
    public void setPdaTreatment(PDA_Treatment pdaTreatment) { this.pdaTreatment = pdaTreatment; }
}
