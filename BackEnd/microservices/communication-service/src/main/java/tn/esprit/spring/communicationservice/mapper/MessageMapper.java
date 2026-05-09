package tn.esprit.spring.communicationservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.spring.communicationservice.domain.entity.FollowUpMessage;
import tn.esprit.spring.communicationservice.domain.entity.MessageAuditLog;
import tn.esprit.spring.communicationservice.domain.entity.MessageReply;
import tn.esprit.spring.communicationservice.dto.response.CreateMessageResponse;
import tn.esprit.spring.communicationservice.dto.response.FollowUpMessageResponse;
import tn.esprit.spring.communicationservice.dto.response.MessageAuditLogResponse;
import tn.esprit.spring.communicationservice.dto.response.MessageReplyResponse;

import java.util.List;

@Component
public class MessageMapper {

    public CreateMessageResponse toCreateResponse(FollowUpMessage entity) {
        return CreateMessageResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .queue(entity.getQueue())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public FollowUpMessageResponse toResponse(FollowUpMessage entity, List<MessageReply> replies) {
        return FollowUpMessageResponse.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .guardianKeycloakId(entity.getGuardianKeycloakId())
                .assignedDoctorKeycloakId(entity.getAssignedDoctorKeycloakId())
                .messageType(entity.getMessageType())
                .priority(entity.getPriority())
                .queue(entity.getQueue())
                .status(entity.getStatus())
                .assignedToUserKeycloakId(entity.getAssignedToUserKeycloakId())
                .assignedToRole(entity.getAssignedToRole())
                .subject(entity.getSubject())
                .messageText(entity.getMessageText())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .lastUpdatedAt(entity.getLastUpdatedAt())
                .closedAt(entity.getClosedAt())
                .replies(replies.stream().map(this::toReplyResponse).toList())
                .build();
    }

    public MessageReplyResponse toReplyResponse(MessageReply entity) {
        return MessageReplyResponse.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .senderKeycloakId(entity.getSenderKeycloakId())
                .senderRole(entity.getSenderRole())
                .replyText(entity.getReplyText())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MessageAuditLogResponse toAuditResponse(MessageAuditLog entity) {
        return MessageAuditLogResponse.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .actorKeycloakId(entity.getActorKeycloakId())
                .actorRole(entity.getActorRole())
                .action(entity.getAction())
                .details(entity.getDetails())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
