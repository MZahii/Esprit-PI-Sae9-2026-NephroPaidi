package tn.esprit.spring.communicationservice.ai;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.domain.enums.AiTriageStatus;
import tn.esprit.spring.communicationservice.domain.enums.AiUrgencyLevel;

import java.time.Instant;

@Getter
@Builder
public class AiTriageResult {
    private AiUrgencyLevel urgencyLevel;
    private Double confidence;
    private AiTriageStatus status;
    private String explanation;
    private String modelVersion;
    private Instant evaluatedAt;

    public static AiTriageResult skipped(String explanation) {
        return AiTriageResult.builder()
                .status(AiTriageStatus.SKIPPED)
                .explanation(explanation)
                .evaluatedAt(Instant.now())
                .build();
    }

    public static AiTriageResult failed(String explanation) {
        return AiTriageResult.builder()
                .status(AiTriageStatus.FAILED)
                .explanation(explanation)
                .evaluatedAt(Instant.now())
                .build();
    }
}
