package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import tn.esprit.spring.clinicalservice.enums.EdemaLocation;
import tn.esprit.spring.clinicalservice.enums.NutritionalAssessment;
import tn.esprit.spring.clinicalservice.enums.BPMeasurementLimb;
import tn.esprit.spring.clinicalservice.enums.BPCuffSize;
import java.math.BigDecimal;

/**
 * Vital signs measurements embeddable.
 * Includes weight, height, BP, heart rate, temperature, O2 saturation, edema assessment.
 */
@Embeddable
public class VitalSigns {
    
    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weight_kg;
    
    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal height_cm;
    
    @Column(name = "head_circumference_cm", precision = 5, scale = 2)
    private BigDecimal headCircumference_cm;
    
    @Column(name = "bp_systolic_mmhg")
    private Integer bpSystolic_mmHg;
    
    @Column(name = "bp_diastolic_mmhg")
    private Integer bpDiastolic_mmHg;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "bp_measurement_limb")
    private BPMeasurementLimb bpMeasurementLimb;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "bp_cuff_size")
    private BPCuffSize bpCuffSize;
    
    @Column(name = "heart_rate_bpm")
    private Integer heartRate_bpm;
    
    @Column(name = "respiratory_rate_bpm")
    private Integer respiratoryRate_bpm;
    
    @Column(name = "temperature_celsius", precision = 4, scale = 1)
    private BigDecimal temperature_C;
    
    @Column(name = "oxygen_saturation_pct")
    private Integer oxygenSaturation_pct;
    
    @Column(name = "edemas_present")
    private Boolean edemasPresent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "edemas_location")
    private EdemaLocation edemasLocation;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "nutritional_assessment")
    private NutritionalAssessment nutritionalAssessment;
    
    // Getters and Setters
    public BigDecimal getWeight_kg() { return weight_kg; }
    public void setWeight_kg(BigDecimal weight_kg) { this.weight_kg = weight_kg; }
    
    public BigDecimal getHeight_cm() { return height_cm; }
    public void setHeight_cm(BigDecimal height_cm) { this.height_cm = height_cm; }
    
    public BigDecimal getHeadCircumference_cm() { return headCircumference_cm; }
    public void setHeadCircumference_cm(BigDecimal headCircumference_cm) { this.headCircumference_cm = headCircumference_cm; }
    
    public Integer getBpSystolic_mmHg() { return bpSystolic_mmHg; }
    public void setBpSystolic_mmHg(Integer bpSystolic_mmHg) { this.bpSystolic_mmHg = bpSystolic_mmHg; }
    
    public Integer getBpDiastolic_mmHg() { return bpDiastolic_mmHg; }
    public void setBpDiastolic_mmHg(Integer bpDiastolic_mmHg) { this.bpDiastolic_mmHg = bpDiastolic_mmHg; }
    
    public BPMeasurementLimb getBpMeasurementLimb() { return bpMeasurementLimb; }
    public void setBpMeasurementLimb(BPMeasurementLimb bpMeasurementLimb) { this.bpMeasurementLimb = bpMeasurementLimb; }
    
    public BPCuffSize getBpCuffSize() { return bpCuffSize; }
    public void setBpCuffSize(BPCuffSize bpCuffSize) { this.bpCuffSize = bpCuffSize; }
    
    public Integer getHeartRate_bpm() { return heartRate_bpm; }
    public void setHeartRate_bpm(Integer heartRate_bpm) { this.heartRate_bpm = heartRate_bpm; }
    
    public Integer getRespiratoryRate_bpm() { return respiratoryRate_bpm; }
    public void setRespiratoryRate_bpm(Integer respiratoryRate_bpm) { this.respiratoryRate_bpm = respiratoryRate_bpm; }
    
    public BigDecimal getTemperature_C() { return temperature_C; }
    public void setTemperature_C(BigDecimal temperature_C) { this.temperature_C = temperature_C; }
    
    public Integer getOxygenSaturation_pct() { return oxygenSaturation_pct; }
    public void setOxygenSaturation_pct(Integer oxygenSaturation_pct) { this.oxygenSaturation_pct = oxygenSaturation_pct; }
    
    public Boolean getEdemasPresent() { return edemasPresent; }
    public void setEdemasPresent(Boolean edemasPresent) { this.edemasPresent = edemasPresent; }
    
    public EdemaLocation getEdemasLocation() { return edemasLocation; }
    public void setEdemasLocation(EdemaLocation edemasLocation) { this.edemasLocation = edemasLocation; }
    
    public NutritionalAssessment getNutritionalAssessment() { return nutritionalAssessment; }
    public void setNutritionalAssessment(NutritionalAssessment nutritionalAssessment) { this.nutritionalAssessment = nutritionalAssessment; }
}
