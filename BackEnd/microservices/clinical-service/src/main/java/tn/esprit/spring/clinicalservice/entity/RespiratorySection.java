package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import tn.esprit.spring.clinicalservice.enums.*;

/**
 * Respiratory pathology section for hospitalization.
 * Gated by hasRespiratoryPathology boolean.
 */
@Embeddable
public class RespiratorySection {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resp_pathology_type")
    private RespiratoryPathologyType respiratoryPathologyType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resp_surfactant_admin")
    private Surfactant surfactantAdministered;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resp_bpd_severity")
    private BPDSeverity bpdSeverity;
    
    @Column(name = "resp_ventilatory_support_28d")
    private Boolean ventilatorySupportAt28d;
    
    @Column(name = "resp_ventilatory_support_36wks")
    private Boolean ventilatorySupportAt36wks;
    
    // Getters and Setters
    public RespiratoryPathologyType getRespiratoryPathologyType() { return respiratoryPathologyType; }
    public void setRespiratoryPathologyType(RespiratoryPathologyType respiratoryPathologyType) { this.respiratoryPathologyType = respiratoryPathologyType; }
    
    public Surfactant getSurfactantAdministered() { return surfactantAdministered; }
    public void setSurfactantAdministered(Surfactant surfactantAdministered) { this.surfactantAdministered = surfactantAdministered; }
    
    public BPDSeverity getBpdSeverity() { return bpdSeverity; }
    public void setBpdSeverity(BPDSeverity bpdSeverity) { this.bpdSeverity = bpdSeverity; }
    
    public Boolean getVentilatorySupportAt28d() { return ventilatorySupportAt28d; }
    public void setVentilatorySupportAt28d(Boolean ventilatorySupportAt28d) { this.ventilatorySupportAt28d = ventilatorySupportAt28d; }
    
    public Boolean getVentilatorySupportAt36wks() { return ventilatorySupportAt36wks; }
    public void setVentilatorySupportAt36wks(Boolean ventilatorySupportAt36wks) { this.ventilatorySupportAt36wks = ventilatorySupportAt36wks; }
}
