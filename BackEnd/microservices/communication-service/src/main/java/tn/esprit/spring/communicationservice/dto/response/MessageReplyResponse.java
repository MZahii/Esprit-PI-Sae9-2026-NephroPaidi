package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.domain.enums.SenderRole;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class MessageReplyResponse {
    private UUID id;
    private UUID messageId;
    private String senderKeycloakId;
    private SenderRole senderRole;
    private String replyText;
    private Instant createdAt;
}
