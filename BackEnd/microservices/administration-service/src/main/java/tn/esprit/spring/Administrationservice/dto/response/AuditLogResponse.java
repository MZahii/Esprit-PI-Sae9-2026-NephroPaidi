package tn.esprit.spring.Administrationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Audit Log Response DTO - represents audit trail entries
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private String action;
    private String actor;
    private String oldValue;
    private String newValue;
    private String createdAt;
    private Long userId;
    private Long scopeId;
}
