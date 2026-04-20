package tn.esprit.spring.clinicalservice.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for audit trail operations
 * Provides endpoints for retrieving audit events
 */
@RestController
@RequestMapping({"/clinical/audit", "/api/clinical/audit"})
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * List all audit events with pagination support
     * @param limit Maximum number of events to return (default: 200, max: 500)
     * @return List of audit events sorted by creation date (descending)
     */
    @GetMapping
    public ResponseEntity<List<AuditEventResponse>> listAuditEvents(
            @RequestParam(value = "limit", defaultValue = "200") int limit
    ) {
        List<AuditEventResponse> responses = auditService.listAuditEvents(limit).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .action(event.getAction())
                .actorId(event.getActorId())
                .actorUsername(event.getActorUsername())
                .actorRole(event.getActorRole())
                .details(event.getDetails())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
