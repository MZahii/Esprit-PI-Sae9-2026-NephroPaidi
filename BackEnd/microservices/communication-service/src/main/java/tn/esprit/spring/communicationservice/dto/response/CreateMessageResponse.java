package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.domain.enums.AiTriageStatus;
import tn.esprit.spring.communicationservice.domain.enums.AiUrgencyLevel;
import tn.esprit.spring.communicationservice.domain.enums.MessageQueue;
import tn.esprit.spring.communicationservice.domain.enums.MessageStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CreateMessageResponse {
    private UUID id;
    private MessageStatus status;
    private MessageQueue queue;
    private Instant createdAt;
    private AiUrgencyLevel aiUrgencyLevel;
    private Double aiConfidence;
    private AiTriageStatus aiTriageStatus;
    private String aiExplanation;
    private String aiModelVersion;
    private Instant aiEvaluatedAt;
}
