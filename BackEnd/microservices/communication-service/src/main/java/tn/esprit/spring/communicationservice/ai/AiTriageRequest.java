package tn.esprit.spring.communicationservice.ai;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.domain.enums.PriorityLevel;

@Getter
@Builder
public class AiTriageRequest {
    private String messageText;
    private String subject;
    private MessageType messageType;
    private PriorityLevel guardianPriority;
}
