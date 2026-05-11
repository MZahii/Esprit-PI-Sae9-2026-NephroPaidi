package tn.esprit.spring.clinicalservice.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO for sending prediction request to AI service
 * Maps ConsultationRecord data to AI model feature expectations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIPredictionRequestDTO {
    private Integer ageYears;
    private String sex;
    private Float creatinine_mg_dl;
    private Float creatinine_umol_l;
    private Float parserConfidence;
    private Boolean has_creatinine;
    private String content_type;
    private Boolean requires_manual_review;
}
