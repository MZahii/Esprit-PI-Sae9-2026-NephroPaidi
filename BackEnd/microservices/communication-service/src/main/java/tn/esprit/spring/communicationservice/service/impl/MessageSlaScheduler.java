package tn.esprit.spring.communicationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.communicationservice.domain.entity.FollowUpMessage;
import tn.esprit.spring.communicationservice.domain.entity.MessageAuditLog;
import tn.esprit.spring.communicationservice.domain.enums.MessageAuditAction;
import tn.esprit.spring.communicationservice.domain.enums.MessageStatus;
import tn.esprit.spring.communicationservice.domain.enums.PriorityLevel;
import tn.esprit.spring.communicationservice.domain.enums.SenderRole;
import tn.esprit.spring.communicationservice.repository.FollowUpMessageRepository;
import tn.esprit.spring.communicationservice.repository.MessageAuditLogRepository;
import tn.esprit.spring.communicationservice.repository.MessageReplyRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageSlaScheduler {

    private final FollowUpMessageRepository followUpMessageRepository;
    private final MessageReplyRepository messageReplyRepository;
    private final MessageAuditLogRepository messageAuditLogRepository;

    @Scheduled(fixedRate = 600_000)
    @Transactional
    public void checkSlaBreaches() {
        Instant now = Instant.now();
        List<FollowUpMessage> highPriority = followUpMessageRepository.findByPriorityAndStatusNot(PriorityLevel.HIGH, MessageStatus.CLOSED);

        for (FollowUpMessage message : highPriority) {
            if (message.getReadAt() == null && Duration.between(message.getCreatedAt(), now).toMinutes() >= 30) {
                if (!messageAuditLogRepository.existsByMessageIdAndAction(message.getId(), MessageAuditAction.SLA_READ_BREACH)) {
                    addSystemAudit(message, MessageAuditAction.SLA_READ_BREACH, "Unread high-priority message beyond 30 minutes");
                }
            }

            boolean hasStaffReply = messageReplyRepository.existsByMessageIdAndSenderRoleIn(
                    message.getId(),
                    List.of(SenderRole.RECEPTIONIST, SenderRole.NURSE, SenderRole.DOCTOR)
            );

            if (!hasStaffReply && Duration.between(message.getCreatedAt(), now).toHours() >= 2) {
                if (!messageAuditLogRepository.existsByMessageIdAndAction(message.getId(), MessageAuditAction.SLA_RESPONSE_BREACH)) {
                    addSystemAudit(message, MessageAuditAction.SLA_RESPONSE_BREACH, "No staff reply to high-priority message beyond 2 hours");
                }
            }
        }
    }

    private void addSystemAudit(FollowUpMessage message, MessageAuditAction action, String details) {
        MessageAuditLog log = new MessageAuditLog();
        log.setMessageId(message.getId());
        log.setActorKeycloakId("SYSTEM");
        log.setActorRole(SenderRole.RECEPTIONIST);
        log.setAction(action);
        log.setDetails(details);
        log.setCreatedAt(Instant.now());
        messageAuditLogRepository.save(log);
    }
}
