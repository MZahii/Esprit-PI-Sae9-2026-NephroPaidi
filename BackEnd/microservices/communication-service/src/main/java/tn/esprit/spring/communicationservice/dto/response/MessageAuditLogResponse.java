package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.domain.enums.MessageAuditAction;
import tn.esprit.spring.communicationservice.domain.enums.SenderRole;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class MessageAuditLogResponse {
    private UUID id;
    private UUID messageId;
    private String actorKeycloakId;
    private SenderRole actorRole;
    private MessageAuditAction action;
    private String details;
    private Instant createdAt;
}
