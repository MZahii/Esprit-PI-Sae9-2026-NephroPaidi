package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import tn.esprit.spring.clinicalservice.enums.*;

/**
 * Infectious disease section for hospitalization.
 * Tracks maternal-fetal infections, infectious agents, antibiotic resistance.
 */
@Embeddable
public class InfectiousSection {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "infect_maternal_fetal_infection")
    private MaternalFetalInfection maternalFetalInfection;
    
    @Column(name = "infect_multi_resistant_bacteria")
    private Boolean multiResistantBacteria;  // HAS mandatory field
    
    @Column(name = "infect_late_infection")
    private Boolean lateInfection;
    
    // Getters and Setters
    public MaternalFetalInfection getMaternalFetalInfection() { return maternalFetalInfection; }
    public void setMaternalFetalInfection(MaternalFetalInfection maternalFetalInfection) { this.maternalFetalInfection = maternalFetalInfection; }
    
    public Boolean getMultiResistantBacteria() { return multiResistantBacteria; }
    public void setMultiResistantBacteria(Boolean multiResistantBacteria) { this.multiResistantBacteria = multiResistantBacteria; }
    
    public Boolean getLateInfection() { return lateInfection; }
    public void setLateInfection(Boolean lateInfection) { this.lateInfection = lateInfection; }
}
