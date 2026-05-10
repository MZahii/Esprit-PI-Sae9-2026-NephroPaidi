package tn.esprit.spring.communicationservice.ai;

import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.communicationservice.domain.enums.AiTriageStatus;
import tn.esprit.spring.communicationservice.domain.enums.AiUrgencyLevel;

@Getter
@Setter
public class AiTriageResponse {
    private AiUrgencyLevel urgencyLevel;
    private Double confidence;
    private AiTriageStatus status;
    private String explanation;
    private String modelVersion;
}
