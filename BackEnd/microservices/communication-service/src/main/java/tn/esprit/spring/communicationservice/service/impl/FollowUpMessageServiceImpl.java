package tn.esprit.spring.communicationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tn.esprit.spring.communicationservice.domain.entity.FollowUpMessage;
import tn.esprit.spring.communicationservice.domain.entity.MessageAuditLog;
import tn.esprit.spring.communicationservice.domain.entity.MessageReply;
import tn.esprit.spring.communicationservice.domain.enums.MessageAuditAction;
import tn.esprit.spring.communicationservice.domain.enums.MessageQueue;
import tn.esprit.spring.communicationservice.domain.enums.MessageStatus;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.domain.enums.PriorityLevel;
import tn.esprit.spring.communicationservice.domain.enums.SenderRole;
import tn.esprit.spring.communicationservice.domain.enums.StaffRole;
import tn.esprit.spring.communicationservice.dto.request.BulkMessageAction;
import tn.esprit.spring.communicationservice.dto.request.BulkMessageOperationRequest;
import tn.esprit.spring.communicationservice.dto.request.CreateMessageRequest;
import tn.esprit.spring.communicationservice.dto.request.EscalateRequest;
import tn.esprit.spring.communicationservice.dto.request.InboxQueryParams;
import tn.esprit.spring.communicationservice.dto.request.ReplyMessageRequest;
import tn.esprit.spring.communicationservice.dto.response.BulkMessageOperationFailureResponse;
import tn.esprit.spring.communicationservice.dto.response.BulkMessageOperationResponse;
import tn.esprit.spring.communicationservice.dto.response.CreateMessageResponse;
import tn.esprit.spring.communicationservice.dto.response.FollowUpMessageResponse;
import tn.esprit.spring.communicationservice.dto.response.MessageAuditLogResponse;
import tn.esprit.spring.communicationservice.exception.BadRequestException;
import tn.esprit.spring.communicationservice.exception.ConflictException;
import tn.esprit.spring.communicationservice.exception.ResourceNotFoundException;
import tn.esprit.spring.communicationservice.mapper.MessageMapper;
import tn.esprit.spring.communicationservice.repository.FollowUpMessageRepository;
import tn.esprit.spring.communicationservice.repository.MessageAuditLogRepository;
import tn.esprit.spring.communicationservice.repository.MessageReplyRepository;
import tn.esprit.spring.communicationservice.security.CurrentUserService;
import tn.esprit.spring.communicationservice.service.FollowUpMessageService;
import tn.esprit.spring.communicationservice.service.GuardianPatientResolverService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowUpMessageServiceImpl implements FollowUpMessageService {

    private final FollowUpMessageRepository followUpMessageRepository;
    private final MessageReplyRepository messageReplyRepository;
    private final MessageAuditLogRepository messageAuditLogRepository;
    private final MessageMapper messageMapper;
    private final CurrentUserService currentUserService;
    private final GuardianPatientResolverService guardianPatientResolverService;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public CreateMessageResponse create(CreateMessageRequest request) {
        currentUserService.requireRole("GUARDIAN");
        Instant now = Instant.now();
        Long resolvedPatientId = guardianPatientResolverService.resolvePatientIdForMessage(request.getPatientId());

        FollowUpMessage entity = new FollowUpMessage();
        entity.setPatientId(resolvedPatientId);
        entity.setGuardianKeycloakId(currentUserService.getCurrentUserSub());
        entity.setMessageType(request.getMessageType());
        entity.setPriority(request.getPriority());
        entity.setSubject(trimOrNull(request.getSubject()));
        entity.setMessageText(request.getMessageText().trim());
        entity.setStatus(MessageStatus.PENDING);
        entity.setQueue(routeQueue(request.getMessageType(), request.getPriority()));
        entity.setCreatedAt(now);
        entity.setLastUpdatedAt(now);

        FollowUpMessage saved = followUpMessageRepository.save(entity);

        addAudit(saved.getId(), MessageAuditAction.CREATED, null);
        addAudit(saved.getId(), MessageAuditAction.ROUTED, "Queue=" + saved.getQueue());

        return messageMapper.toCreateResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpMessageResponse> myMessages() {
        currentUserService.requireRole("GUARDIAN");
        String guardianSub = currentUserService.getCurrentUserSub();
        return followUpMessageRepository.findByGuardianKeycloakIdOrderByCreatedAtDesc(guardianSub)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUpMessageResponse getById(UUID id) {
        FollowUpMessage message = getMessageOrThrow(id);
        assertCanAccess(message);
        return toMessageResponse(message);
    }

    @Override
    @Transactional
    public FollowUpMessageResponse take(UUID id) {
        StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
        String sub = currentUserService.getCurrentUserSub();
        FollowUpMessage message = getMessageOrThrow(id);
        assertCanAccessAsStaff(message, staffRole, sub);
        assertOpen(message, "take");

        if (message.getAssignedToUserKeycloakId() == null) {
            message.setAssignedToUserKeycloakId(sub);
            message.setAssignedToRole(staffRole);
            message.setStatus(MessageStatus.IN_PROGRESS);
            message.setLastUpdatedAt(Instant.now());
            followUpMessageRepository.save(message);
            addAudit(message.getId(), MessageAuditAction.TAKEN, "Taken by=" + sub);
        } else if (!message.getAssignedToUserKeycloakId().equals(sub)) {
            throw new ConflictException("Message is already taken by another staff member");
        }

        return toMessageResponse(message);
    }

    @Override
    @Transactional
    public FollowUpMessageResponse unassign(UUID id) {
        StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
        String sub = currentUserService.getCurrentUserSub();
        FollowUpMessage message = getMessageOrThrow(id);
        assertCanAccessAsStaff(message, staffRole, sub);
        assertOpen(message, "unassign");

        if (message.getAssignedToUserKeycloakId() == null) {
            throw new ConflictException("Message is not assigned");
        }

        message.setAssignedToUserKeycloakId(null);
        message.setAssignedToRole(null);
        if (message.getStatus() == MessageStatus.IN_PROGRESS) {
            message.setStatus(MessageStatus.READ);
        }
        message.setLastUpdatedAt(Instant.now());
        followUpMessageRepository.save(message);
        addAudit(message.getId(), MessageAuditAction.UNASSIGNED, "Unassigned by=" + sub);

        return toMessageResponse(message);
    }

    @Override
    @Transactional
    public FollowUpMessageResponse markRead(UUID id) {
        StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
        String sub = currentUserService.getCurrentUserSub();
        FollowUpMessage message = getMessageOrThrow(id);
        assertCanAccessAsStaff(message, staffRole, sub);
        assertOpen(message, "mark read");

        if (message.getReadAt() == null) {
            message.setReadAt(Instant.now());
        }

        if (message.getStatus() == MessageStatus.PENDING) {
            message.setStatus(MessageStatus.READ);
        }

        message.setLastUpdatedAt(Instant.now());
        followUpMessageRepository.save(message);
        addAudit(message.getId(), MessageAuditAction.MARK_READ, null);

        return toMessageResponse(message);
    }

    @Override
    @Transactional
    public FollowUpMessageResponse reply(UUID id, ReplyMessageRequest request) {
        FollowUpMessage message = getMessageOrThrow(id);
        assertCanAccess(message);
        assertOpen(message, "reply to");

        SenderRole senderRole = currentUserService.getSenderRoleOrThrow();
        String senderSub = currentUserService.getCurrentUserSub();

        MessageReply reply = new MessageReply();
        reply.setMessageId(message.getId());
        reply.setSenderKeycloakId(senderSub);
        reply.setSenderRole(senderRole);
        reply.setReplyText(request.getReplyText().trim());
        reply.setCreatedAt(Instant.now());
        messageReplyRepository.save(reply);

        if (senderRole != SenderRole.GUARDIAN) {
            message.setStatus(MessageStatus.RESPONDED);
        }

        message.setLastUpdatedAt(Instant.now());
        followUpMessageRepository.save(message);
        addAudit(message.getId(), MessageAuditAction.REPLIED, "SenderRole=" + senderRole);

        return toMessageResponse(message);
    }

    @Override
    @Transactional
    public FollowUpMessageResponse escalate(UUID id, EscalateRequest request) {
        currentUserService.requireRole("NURSE");
        String sub = currentUserService.getCurrentUserSub();
        FollowUpMessage message = getMessageOrThrow(id);
        assertCanAccessAsStaff(message, StaffRole.NURSE, sub);
        assertOpen(message, "escalate");

        String doctorId = trimOrNull(request.getDoctorKeycloakId());
        if (doctorId == null) {
            throw new BadRequestException("doctorKeycloakId is required");
        }

        String reason = trimOrNull(request.getReason());
        if (reason == null) {
            throw new BadRequestException("Escalation reason is required");
        }
        if (reason.length() > 500) {
            throw new BadRequestException("Escalation reason must not exceed 500 characters");
        }

        message.setAssignedDoctorKeycloakId(doctorId);
        message.setQueue(MessageQueue.DOCTOR);
        message.setAssignedToRole(StaffRole.DOCTOR);
        message.setAssignedToUserKeycloakId(doctorId);
        message.setStatus(MessageStatus.ESCALATED);
        message.setLastUpdatedAt(Instant.now());
        followUpMessageRepository.save(message);
        addAudit(message.getId(), MessageAuditAction.ESCALATED,
                "Escalated to doctor=" + doctorId + "; reason=" + reason);

        return toMessageResponse(message);
    }

    @Override
    @Transactional
    public FollowUpMessageResponse close(UUID id) {
        FollowUpMessage message = getMessageOrThrow(id);
        String sub = currentUserService.getCurrentUserSub();

        if (message.getStatus() == MessageStatus.CLOSED) {
            throw new ConflictException("Message is already closed");
        }

        if (currentUserService.isGuardian()) {
            if (!message.getGuardianKeycloakId().equals(sub)) {
                throw new AccessDeniedException("Guardian can only close their own messages");
            }
        } else {
            StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
            MessageQueue queue = MessageQueue.valueOf(staffRole.name());

            boolean queueMatches = message.getQueue() == queue;
            boolean assignedToCurrent = sub.equals(message.getAssignedToUserKeycloakId());
            if (!queueMatches && !assignedToCurrent) {
                throw new AccessDeniedException("Staff can close only queue-matching or assigned messages");
            }
        }

        message.setStatus(MessageStatus.CLOSED);
        message.setClosedAt(Instant.now());
        message.setLastUpdatedAt(Instant.now());
        followUpMessageRepository.save(message);
        addAudit(message.getId(), MessageAuditAction.CLOSED, null);

        return toMessageResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpMessageResponse> staffInbox(InboxQueryParams params) {
        StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
        MessageQueue roleQueue = MessageQueue.valueOf(staffRole.name());
        MessageQueue queue = params.getQueue() != null ? params.getQueue() : roleQueue;

        if (queue != roleQueue) {
            throw new AccessDeniedException("Staff can only access their own queue");
        }

        Specification<FollowUpMessage> specification =
                (root, query, cb) -> cb.equal(root.get("queue"), queue);

        if (params.getStatus() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), params.getStatus()));
        }
        if (params.getPriority() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("priority"), params.getPriority()));
        }
        if (params.getMessageType() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("messageType"), params.getMessageType()));
        }
        if (params.getPatientId() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("patientId"), params.getPatientId()));
        }
        if (params.getDateFrom() != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), params.getDateFrom()));
        }
        if (params.getDateTo() != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), params.getDateTo()));
        }

        return followUpMessageRepository.findAll(specification).stream()
                .sorted((a, b) -> {
                    if (staffRole == StaffRole.NURSE && a.getPriority() != b.getPriority()) {
                        if (a.getPriority().name().equals("HIGH")) return -1;
                        if (b.getPriority().name().equals("HIGH")) return 1;
                    }
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageAuditLogResponse> audit(UUID id) {
        FollowUpMessage message = getMessageOrThrow(id);
        assertCanAccess(message);

        return messageAuditLogRepository.findByMessageIdOrderByCreatedAtAsc(id)
                .stream()
                .map(messageMapper::toAuditResponse)
                .toList();
    }

    @Override
    public BulkMessageOperationResponse bulkOperate(BulkMessageOperationRequest request) {
        currentUserService.getStaffRoleOrThrow();

        List<UUID> messageIds = request.getMessageIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (messageIds.isEmpty()) {
            throw new BadRequestException("At least one messageId is required");
        }

        List<UUID> succeededIds = new ArrayList<>();
        List<BulkMessageOperationFailureResponse> failures = new ArrayList<>();

        for (UUID messageId : messageIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> applyBulkAction(messageId, request.getAction()));
                succeededIds.add(messageId);
            } catch (Exception ex) {
                failures.add(BulkMessageOperationFailureResponse.builder()
                        .messageId(messageId)
                        .error(resolveErrorMessage(ex))
                        .build());
            }
        }

        return BulkMessageOperationResponse.builder()
                .action(request.getAction())
                .requestedCount(messageIds.size())
                .successCount(succeededIds.size())
                .failedCount(failures.size())
                .succeededIds(succeededIds)
                .failures(failures)
                .build();
    }

    private MessageQueue routeQueue(MessageType type, PriorityLevel priority) {
        if (type == MessageType.ADMINISTRATIVE || type == MessageType.APPOINTMENT) {
            return MessageQueue.RECEPTIONIST;
        }
        if (type == MessageType.MEDICAL || type == MessageType.LAB_RESULT || type == MessageType.OTHER) {
            return MessageQueue.NURSE;
        }
        if ((type == MessageType.QUESTION || type == MessageType.COMPLAINT) && priority == PriorityLevel.HIGH) {
            return MessageQueue.NURSE;
        }
        if (type == MessageType.QUESTION || type == MessageType.COMPLAINT) {
            return MessageQueue.RECEPTIONIST;
        }
        return MessageQueue.NURSE;
    }

    private FollowUpMessage getMessageOrThrow(UUID id) {
        return followUpMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + id));
    }

    private FollowUpMessageResponse toMessageResponse(FollowUpMessage message) {
        List<MessageReply> replies = messageReplyRepository.findByMessageIdOrderByCreatedAtAsc(message.getId());
        return messageMapper.toResponse(message, replies);
    }

    private void assertCanAccess(FollowUpMessage message) {
        String sub = currentUserService.getCurrentUserSub();

        if (currentUserService.isGuardian()) {
            if (!message.getGuardianKeycloakId().equals(sub)) {
                throw new AccessDeniedException("Guardian can only access their own messages");
            }
            return;
        }

        StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
        assertCanAccessAsStaff(message, staffRole, sub);
    }

    private void assertCanAccessAsStaff(FollowUpMessage message, StaffRole staffRole, String sub) {
        MessageQueue queue = MessageQueue.valueOf(staffRole.name());
        boolean queueMatches = message.getQueue() == queue;
        boolean assignedToCurrent = sub.equals(message.getAssignedToUserKeycloakId());
        if (!queueMatches && !assignedToCurrent) {
            throw new AccessDeniedException("Staff access denied for this message");
        }
    }

    private void assertOpen(FollowUpMessage message, String action) {
        if (message.getStatus() == MessageStatus.CLOSED) {
            throw new ConflictException("Cannot " + action + " a closed message");
        }
    }

    private void addAudit(UUID messageId, MessageAuditAction action, String details) {
        MessageAuditLog log = new MessageAuditLog();
        log.setMessageId(messageId);
        log.setActorKeycloakId(currentUserService.getCurrentUserSub());
        log.setActorRole(currentUserService.getSenderRoleOrThrow());
        log.setAction(action);
        log.setDetails(details);
        log.setCreatedAt(Instant.now());
        messageAuditLogRepository.save(log);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyBulkAction(UUID messageId, BulkMessageAction action) {
        switch (action) {
            case TAKE -> take(messageId);
            case MARK_READ -> markRead(messageId);
            case UNASSIGN -> unassign(messageId);
            case CLOSE -> close(messageId);
            default -> throw new BadRequestException("Unsupported bulk action: " + action);
        }
    }

    private String resolveErrorMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return "Operation failed";
        }
        return message;
    }
}
