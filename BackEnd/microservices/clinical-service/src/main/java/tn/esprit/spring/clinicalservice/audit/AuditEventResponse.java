package tn.esprit.spring.clinicalservice.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AuditEventResponse {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String action;
    private String actorId;
    private String actorUsername;
    private String actorRole;
    private String details;
    private LocalDateTime createdAt;
}
