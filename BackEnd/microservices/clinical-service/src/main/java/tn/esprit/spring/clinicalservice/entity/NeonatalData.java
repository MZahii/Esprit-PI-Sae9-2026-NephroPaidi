package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import tn.esprit.spring.clinicalservice.enums.*;
import java.math.BigDecimal;

/**
 * Neonatal data embeddable for hospitalization records.
 * Captures birth-related information for premature/neonatal patients.
 */
@Embeddable
public class NeonatalData {
    
    @Column(name = "neonatal_gestational_age_weeks")
    private Integer gestationalAgeAtBirth_weeks;
    
    @Column(name = "neonatal_birth_weight_g")
    private Integer birthWeight_g;
    
    @Column(name = "neonatal_birth_length_cm", precision = 5, scale = 2)
    private BigDecimal birthLength_cm;
    
    @Column(name = "neonatal_birth_head_circumference_cm", precision = 5, scale = 2)
    private BigDecimal birthHeadCircumference_cm;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "neonatal_hypotrophy")
    private Hypotrophy hypotrophy;
    
    @Column(name = "neonatal_apgar_1min")
    private Integer apgarScore1min;
    
    @Column(name = "neonatal_apgar_5min")
    private Integer apgarScore5min;
    
    @Column(name = "neonatal_apgar_10min")
    private Integer apgarScore10min;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "neonatal_pregnancy_type")
    private PregnancyType pregnancyType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "neonatal_delivery_mode")
    private DeliveryMode deliveryMode;
    
    @Column(name = "neonatal_delivery_induced")
    private Boolean deliveryInduced;
    
    // Getters and Setters
    public Integer getGestationalAgeAtBirth_weeks() { return gestationalAgeAtBirth_weeks; }
    public void setGestationalAgeAtBirth_weeks(Integer gestationalAgeAtBirth_weeks) { this.gestationalAgeAtBirth_weeks = gestationalAgeAtBirth_weeks; }
    
    public Integer getBirthWeight_g() { return birthWeight_g; }
    public void setBirthWeight_g(Integer birthWeight_g) { this.birthWeight_g = birthWeight_g; }
    
    public BigDecimal getBirthLength_cm() { return birthLength_cm; }
    public void setBirthLength_cm(BigDecimal birthLength_cm) { this.birthLength_cm = birthLength_cm; }
    
    public BigDecimal getBirthHeadCircumference_cm() { return birthHeadCircumference_cm; }
    public void setBirthHeadCircumference_cm(BigDecimal birthHeadCircumference_cm) { this.birthHeadCircumference_cm = birthHeadCircumference_cm; }
    
    public Hypotrophy getHypotrophy() { return hypotrophy; }
    public void setHypotrophy(Hypotrophy hypotrophy) { this.hypotrophy = hypotrophy; }
    
    public Integer getApgarScore1min() { return apgarScore1min; }
    public void setApgarScore1min(Integer apgarScore1min) { this.apgarScore1min = apgarScore1min; }
    
    public Integer getApgarScore5min() { return apgarScore5min; }
    public void setApgarScore5min(Integer apgarScore5min) { this.apgarScore5min = apgarScore5min; }
    
    public Integer getApgarScore10min() { return apgarScore10min; }
    public void setApgarScore10min(Integer apgarScore10min) { this.apgarScore10min = apgarScore10min; }
    
    public PregnancyType getPregnancyType() { return pregnancyType; }
    public void setPregnancyType(PregnancyType pregnancyType) { this.pregnancyType = pregnancyType; }
    
    public DeliveryMode getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(DeliveryMode deliveryMode) { this.deliveryMode = deliveryMode; }
    
    public Boolean getDeliveryInduced() { return deliveryInduced; }
    public void setDeliveryInduced(Boolean deliveryInduced) { this.deliveryInduced = deliveryInduced; }
}
