package tn.esprit.spring.communicationservice.staffmessaging.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffConversation;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffConversationParticipant;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffMessage;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffConversationSummaryResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessagingSocketEvent;
import tn.esprit.spring.communicationservice.staffmessaging.repository.StaffConversationParticipantRepository;
import tn.esprit.spring.communicationservice.staffmessaging.repository.StaffConversationRepository;
import tn.esprit.spring.communicationservice.staffmessaging.repository.StaffMessageRepository;
import tn.esprit.spring.communicationservice.staffmessaging.service.StaffMessagingMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StaffMessagingRealtimeNotifier {

    private final StaffConversationRepository conversationRepository;
    private final StaffConversationParticipantRepository participantRepository;
    private final StaffMessageRepository messageRepository;
    private final StaffMessagingMapper mapper;
    private final StaffMessagingRealtimeGateway realtimeGateway;

    public void notifyMessageCreated(UUID conversationId, UUID messageId) {
        StaffConversation conversation = conversationRepository.findById(conversationId).orElse(null);
        StaffMessage message = messageRepository.findById(messageId).orElse(null);
        if (conversation == null || message == null) {
            return;
        }

        List<StaffConversationParticipant> participants = participantRepository.findByConversation_IdOrderByJoinedAtAsc(conversationId);
        for (StaffConversationParticipant participant : participants) {
            StaffConversationSummaryResponse summary = buildSummary(conversation, participant, participants, message);
            realtimeGateway.sendToUser(participant.getUserId(), StaffMessagingSocketEvent.builder()
                    .type(StaffMessagingSocketEventType.MESSAGE_CREATED)
                    .conversationId(conversationId)
                    .conversation(summary)
                    .message(mapper.toMessageResponse(message))
                    .build());
        }
    }

    public void notifyConversationRead(UUID conversationId, String userId) {
        StaffConversation conversation = conversationRepository.findById(conversationId).orElse(null);
        StaffConversationParticipant currentParticipant = participantRepository.findByConversation_IdAndUserId(conversationId, userId).orElse(null);
        if (conversation == null || currentParticipant == null) {
            return;
        }

        List<StaffConversationParticipant> participants = participantRepository.findByConversation_IdOrderByJoinedAtAsc(conversationId);
        StaffMessage lastMessage = messageRepository.findTopByConversation_IdAndDeletedAtIsNullOrderByCreatedAtDesc(conversationId).orElse(null);
        StaffConversationSummaryResponse summary = buildSummary(conversation, currentParticipant, participants, lastMessage);

        realtimeGateway.sendToUser(userId, StaffMessagingSocketEvent.builder()
                .type(StaffMessagingSocketEventType.CONVERSATION_READ)
                .conversationId(conversationId)
                .conversation(summary)
                .message(lastMessage == null ? null : mapper.toMessageResponse(lastMessage))
                .build());
    }

    private StaffConversationSummaryResponse buildSummary(
            StaffConversation conversation,
            StaffConversationParticipant currentParticipant,
            List<StaffConversationParticipant> participants,
            StaffMessage lastMessage
    ) {
        long unreadCount = countUnreadMessages(conversation.getId(), currentParticipant.getUserId(), currentParticipant.getLastReadAt());
        return mapper.toConversationSummary(conversation, unreadCount, lastMessage, participants);
    }

    private long countUnreadMessages(UUID conversationId, String currentUserId, Instant lastReadAt) {
        if (lastReadAt == null) {
            return messageRepository.countByConversation_IdAndSenderIdNotAndDeletedAtIsNull(conversationId, currentUserId);
        }

        return messageRepository.countByConversation_IdAndSenderIdNotAndCreatedAtAfterAndDeletedAtIsNull(
                conversationId,
                currentUserId,
                lastReadAt
        );
    }
}
