package tn.esprit.spring.communicationservice.staffmessaging.service;

import org.springframework.stereotype.Component;
import tn.esprit.spring.communicationservice.integration.dto.UserSummary;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffConversation;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffConversationParticipant;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffMessage;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffConversationParticipantResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffConversationSummaryResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessageResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessagingUserResponse;

import java.util.List;

@Component
public class StaffMessagingMapper {

    public StaffMessagingUserResponse toUserResponse(UserSummary user) {
        return StaffMessagingUserResponse.builder()
                .userId(user.getKeycloakId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(InternalStaffRole.fromValue(user.getRole()))
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public StaffConversationSummaryResponse toConversationSummary(
            StaffConversation conversation,
            long unreadCount,
            StaffMessage lastMessage,
            List<StaffConversationParticipant> participants
    ) {
        return StaffConversationSummaryResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .title(conversation.getTitle())
                .createdByUserId(conversation.getCreatedByUserId())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unreadCount)
                .lastMessage(lastMessage == null ? null : toMessageResponse(lastMessage))
                .participants(participants.stream().map(this::toParticipantResponse).toList())
                .build();
    }

    public StaffConversationParticipantResponse toParticipantResponse(StaffConversationParticipant participant) {
        return StaffConversationParticipantResponse.builder()
                .userId(participant.getUserId())
                .userRole(participant.getUserRole())
                .displayName(participant.getDisplayName())
                .joinedAt(participant.getJoinedAt())
                .lastReadAt(participant.getLastReadAt())
                .archived(participant.isArchived())
                .muted(participant.isMuted())
                .build();
    }

    public StaffMessageResponse toMessageResponse(StaffMessage message) {
        return StaffMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSenderId())
                .senderRole(message.getSenderRole())
                .senderDisplayName(message.getSenderDisplayName())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .deletedAt(message.getDeletedAt())
                .build();
    }
}
