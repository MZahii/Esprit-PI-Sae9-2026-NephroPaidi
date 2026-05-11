package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import tn.esprit.spring.clinicalservice.enums.*;

/**
 * Auditory and vision screening section for hospitalization.
 * Tracks hearing test results and retinopathy of prematurity (ROP).
 */
@Embeddable
public class AuditoryVisionSection {
    
    @Column(name = "audit_screening_status")
    private Boolean hearingScreeningStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_hearing_result")
    private HearingResult hearingResult;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_hearing_coding_score")
    private HearingScore hearingCodingScore;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vision_rop_stage")
    private ROP_Stage ropStage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vision_rop_treatment")
    private ROP_Treatment ropTreatment;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vision_coding_score")
    private VisionScore visionCodingScore;
    
    // Getters and Setters
    public Boolean getHearingScreeningStatus() { return hearingScreeningStatus; }
    public void setHearingScreeningStatus(Boolean hearingScreeningStatus) { this.hearingScreeningStatus = hearingScreeningStatus; }
    
    public HearingResult getHearingResult() { return hearingResult; }
    public void setHearingResult(HearingResult hearingResult) { this.hearingResult = hearingResult; }
    
    public HearingScore getHearingCodingScore() { return hearingCodingScore; }
    public void setHearingCodingScore(HearingScore hearingCodingScore) { this.hearingCodingScore = hearingCodingScore; }
    
    public ROP_Stage getRopStage() { return ropStage; }
    public void setRopStage(ROP_Stage ropStage) { this.ropStage = ropStage; }
    
    public ROP_Treatment getRopTreatment() { return ropTreatment; }
    public void setRopTreatment(ROP_Treatment ropTreatment) { this.ropTreatment = ropTreatment; }
    
    public VisionScore getVisionCodingScore() { return visionCodingScore; }
    public void setVisionCodingScore(VisionScore visionCodingScore) { this.visionCodingScore = visionCodingScore; }
}
