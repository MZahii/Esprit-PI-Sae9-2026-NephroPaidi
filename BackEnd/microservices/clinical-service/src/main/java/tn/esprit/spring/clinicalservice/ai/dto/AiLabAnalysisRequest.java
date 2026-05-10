package tn.esprit.spring.clinicalservice.ai.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AiLabAnalysisRequest {
    private UUID consultationId;
    private Long patientId;
    private Integer patientAgeYears;
    private String patientSex;
    private Double heightCm;
    private Double weightKg;
    private String documentType;
    private String fileName;
    private String contentType;
    private String fileContentBase64;
}
