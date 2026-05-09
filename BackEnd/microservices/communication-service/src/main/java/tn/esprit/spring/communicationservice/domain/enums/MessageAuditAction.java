package tn.esprit.spring.communicationservice.domain.enums;

public enum MessageAuditAction {
    CREATED,
    ROUTED,
    TAKEN,
    UNASSIGNED,
    MARK_READ,
    REPLIED,
    ESCALATED,
    CLOSED,
    SLA_READ_BREACH,
    SLA_RESPONSE_BREACH
}
