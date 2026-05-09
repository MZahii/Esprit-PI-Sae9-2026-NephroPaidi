package tn.esprit.spring.clinicalservice.ai.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiLabAnalysisResponse {
    private String status;
    private String recommendation;
    private Double confidence;
    private String summary;
    private Boolean requiresDoctorReview;
    private Double extractedCreatinineMgDl;
    private Double extractedCreatinineUmolL;
    private String extractedUnit;
    private Double parserConfidence;
    private List<String> parserWarnings;
    private String modelUsed;
}
