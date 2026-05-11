package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import tn.esprit.spring.clinicalservice.enums.*;
import java.math.BigDecimal;

/**
 * Pediatric nephrology clinical findings (embeddable record).
 * Includes renal function, urinalysis, biomarkers, and diagnostic sections.
 * Embedded into ConsultationRecord and HospitalizationRecord.
 */
@Embeddable
public class PediatricNephrologyRecord {
    
    // ============= RENAL FUNCTION SECTION =============
    @Column(name = "egfr", precision = 8, scale = 2)
    private BigDecimal eGFR;  // computed via Schwartz formula
    
    @Column(name = "schwartz_k", precision = 4, scale = 2)
    private BigDecimal schwartz_k;  // age-dependent k-value (0.33, 0.45, 0.55, 0.70)
    
    @Column(name = "serum_creatinine_umol_l", precision = 8, scale = 2)
    private BigDecimal serumCreatinine_umolL;
    
    @Column(name = "serum_creatinine_mg_dl", precision = 8, scale = 2)
    private BigDecimal serumCreatinine_mgdL;
    
    @Column(name = "cystatin_c_mg_l", precision = 8, scale = 2)
    private BigDecimal cystatinC_mgL;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ckd_stage")
    private CKDStage ckdStage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ckd_cause")
    private CKDCause ckdCause;
    
    // ============= URINALYSIS SECTION =============
    @Column(name = "urine_protein_creatinine_ratio", precision = 8, scale = 2)
    private BigDecimal urineProteinCreatinineRatio;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "proteinuria_category")
    private ProteinuriaCategory proteinuriaCategory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "hematuria_level")
    private HematuriaLevel hematuria;
    
    @Column(name = "dysmorphic_erythrocytes")
    private Boolean dysmorphicErythrocytes;
    
    @Column(name = "leukocyturia")
    private Boolean leukocyturia;
    
    // ============= BIOMARKERS SECTION =============
    @Column(name = "serum_albumin_g_l", precision = 8, scale = 2)
    private BigDecimal serumAlbumin_g_L;
    
    @Column(name = "serum_sodium_mmol_l", precision = 8, scale = 2)
    private BigDecimal serumSodium_mmolL;
    
    @Column(name = "serum_potassium_mmol_l", precision = 8, scale = 2)
    private BigDecimal serumPotassium_mmolL;
    
    @Column(name = "serum_bicarbonate_mmol_l", precision = 8, scale = 2)
    private BigDecimal serumBicarbonate_mmolL;
    
    @Column(name = "serum_phosphate_mmol_l", precision = 8, scale = 2)
    private BigDecimal serumPhosphate_mmolL;
    
    @Column(name = "serum_calcium_mmol_l", precision = 8, scale = 2)
    private BigDecimal serumCalcium_mmolL;
    
    @Column(name = "pth_pg_ml", precision = 8, scale = 2)
    private BigDecimal pth_pg_mL;
    
    @Column(name = "vitamin_d_25oh_nmol_l", precision = 8, scale = 2)
    private BigDecimal vitaminD_25OH_nmolL;
    
    @Column(name = "serum_urea_mmol_l", precision = 8, scale = 2)
    private BigDecimal serumUrea_mmolL;
    
    // ============= PLACEHOLDER SECTIONS (DAY 2 EXPANSION) =============
    @Column(name = "hus_present")
    private Boolean husPresent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "hus_type")
    private HUSType husType;  // defer HUS section details to Day 2
    
    @Column(name = "aki_present")
    private Boolean akiPresent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "aki_origin")
    private AKIOrigin akiOrigin;  // defer AKI section details to Day 2
    
    @Column(name = "nephrotic_syndrome")
    private Boolean nephroticSyndrome;  // defer nephrotic details to Day 2
    
    @Column(name = "hereditary_nephropathy")
    private Boolean hereditaryNephropathy;  // defer hereditary section to Day 2
    
    // ============= GETTERS & SETTERS =============
    public BigDecimal getEGFR() { return eGFR; }
    public void setEGFR(BigDecimal eGFR) { this.eGFR = eGFR; }
    
    public BigDecimal getSchwartz_k() { return schwartz_k; }
    public void setSchwartz_k(BigDecimal schwartz_k) { this.schwartz_k = schwartz_k; }
    
    public BigDecimal getSerumCreatinine_umolL() { return serumCreatinine_umolL; }
    public void setSerumCreatinine_umolL(BigDecimal serumCreatinine_umolL) { this.serumCreatinine_umolL = serumCreatinine_umolL; }
    
    public BigDecimal getSerumCreatinine_mgdL() { return serumCreatinine_mgdL; }
    public void setSerumCreatinine_mgdL(BigDecimal serumCreatinine_mgdL) { this.serumCreatinine_mgdL = serumCreatinine_mgdL; }
    
    public BigDecimal getCystatinC_mgL() { return cystatinC_mgL; }
    public void setCystatinC_mgL(BigDecimal cystatinC_mgL) { this.cystatinC_mgL = cystatinC_mgL; }
    
    public CKDStage getCkdStage() { return ckdStage; }
    public void setCkdStage(CKDStage ckdStage) { this.ckdStage = ckdStage; }
    
    public CKDCause getCkdCause() { return ckdCause; }
    public void setCkdCause(CKDCause ckdCause) { this.ckdCause = ckdCause; }
    
    public BigDecimal getUrineProteinCreatinineRatio() { return urineProteinCreatinineRatio; }
    public void setUrineProteinCreatinineRatio(BigDecimal urineProteinCreatinineRatio) { this.urineProteinCreatinineRatio = urineProteinCreatinineRatio; }
    
    public ProteinuriaCategory getProteinuriaCategory() { return proteinuriaCategory; }
    public void setProteinuriaCategory(ProteinuriaCategory proteinuriaCategory) { this.proteinuriaCategory = proteinuriaCategory; }
    
    public HematuriaLevel getHematuria() { return hematuria; }
    public void setHematuria(HematuriaLevel hematuria) { this.hematuria = hematuria; }
    
    public Boolean getDysmorphicErythrocytes() { return dysmorphicErythrocytes; }
    public void setDysmorphicErythrocytes(Boolean dysmorphicErythrocytes) { this.dysmorphicErythrocytes = dysmorphicErythrocytes; }
    
    public Boolean getLeukocyturia() { return leukocyturia; }
    public void setLeukocyturia(Boolean leukocyturia) { this.leukocyturia = leukocyturia; }
    
    public BigDecimal getSerumAlbumin_g_L() { return serumAlbumin_g_L; }
    public void setSerumAlbumin_g_L(BigDecimal serumAlbumin_g_L) { this.serumAlbumin_g_L = serumAlbumin_g_L; }
    
    public BigDecimal getSerumSodium_mmolL() { return serumSodium_mmolL; }
    public void setSerumSodium_mmolL(BigDecimal serumSodium_mmolL) { this.serumSodium_mmolL = serumSodium_mmolL; }
    
    public BigDecimal getSerumPotassium_mmolL() { return serumPotassium_mmolL; }
    public void setSerumPotassium_mmolL(BigDecimal serumPotassium_mmolL) { this.serumPotassium_mmolL = serumPotassium_mmolL; }
    
    public BigDecimal getSerumBicarbonate_mmolL() { return serumBicarbonate_mmolL; }
    public void setSerumBicarbonate_mmolL(BigDecimal serumBicarbonate_mmolL) { this.serumBicarbonate_mmolL = serumBicarbonate_mmolL; }
    
    public BigDecimal getSerumPhosphate_mmolL() { return serumPhosphate_mmolL; }
    public void setSerumPhosphate_mmolL(BigDecimal serumPhosphate_mmolL) { this.serumPhosphate_mmolL = serumPhosphate_mmolL; }
    
    public BigDecimal getSerumCalcium_mmolL() { return serumCalcium_mmolL; }
    public void setSerumCalcium_mmolL(BigDecimal serumCalcium_mmolL) { this.serumCalcium_mmolL = serumCalcium_mmolL; }
    
    public BigDecimal getPth_pg_mL() { return pth_pg_mL; }
    public void setPth_pg_mL(BigDecimal pth_pg_mL) { this.pth_pg_mL = pth_pg_mL; }
    
    public BigDecimal getVitaminD_25OH_nmolL() { return vitaminD_25OH_nmolL; }
    public void setVitaminD_25OH_nmolL(BigDecimal vitaminD_25OH_nmolL) { this.vitaminD_25OH_nmolL = vitaminD_25OH_nmolL; }
    
    public BigDecimal getSerumUrea_mmolL() { return serumUrea_mmolL; }
    public void setSerumUrea_mmolL(BigDecimal serumUrea_mmolL) { this.serumUrea_mmolL = serumUrea_mmolL; }
    
    public Boolean getHusPresent() { return husPresent; }
    public void setHusPresent(Boolean husPresent) { this.husPresent = husPresent; }
    
    public HUSType getHusType() { return husType; }
    public void setHusType(HUSType husType) { this.husType = husType; }
    
    public Boolean getAkiPresent() { return akiPresent; }
    public void setAkiPresent(Boolean akiPresent) { this.akiPresent = akiPresent; }
    
    public AKIOrigin getAkiOrigin() { return akiOrigin; }
    public void setAkiOrigin(AKIOrigin akiOrigin) { this.akiOrigin = akiOrigin; }
    
    public Boolean getNephroticSyndrome() { return nephroticSyndrome; }
    public void setNephroticSyndrome(Boolean nephroticSyndrome) { this.nephroticSyndrome = nephroticSyndrome; }
    
    public Boolean getHereditaryNephropathy() { return hereditaryNephropathy; }
    public void setHereditaryNephropathy(Boolean hereditaryNephropathy) { this.hereditaryNephropathy = hereditaryNephropathy; }
}
