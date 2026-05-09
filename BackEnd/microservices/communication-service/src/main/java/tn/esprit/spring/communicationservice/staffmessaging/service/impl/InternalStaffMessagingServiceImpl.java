package tn.esprit.spring.communicationservice.staffmessaging.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.communicationservice.exception.BadRequestException;
import tn.esprit.spring.communicationservice.exception.ResourceNotFoundException;
import tn.esprit.spring.communicationservice.integration.UserDirectoryClient;
import tn.esprit.spring.communicationservice.integration.dto.UserSummary;
import tn.esprit.spring.communicationservice.security.CurrentUserService;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;
import tn.esprit.spring.communicationservice.staffmessaging.domain.StaffConversationType;
import tn.esprit.spring.communicationservice.staffmessaging.domain.StaffMessageType;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffConversation;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffConversationParticipant;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffMessage;
import tn.esprit.spring.communicationservice.staffmessaging.dto.request.CreateDirectStaffConversationRequest;
import tn.esprit.spring.communicationservice.staffmessaging.dto.request.SendStaffMessageRequest;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffConversationSummaryResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessageResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessagingUserResponse;
import tn.esprit.spring.communicationservice.staffmessaging.repository.StaffConversationParticipantRepository;
import tn.esprit.spring.communicationservice.staffmessaging.repository.StaffConversationRepository;
import tn.esprit.spring.communicationservice.staffmessaging.repository.StaffMessageRepository;
import tn.esprit.spring.communicationservice.staffmessaging.service.InternalStaffMessagingService;
import tn.esprit.spring.communicationservice.staffmessaging.service.StaffMessagingMapper;
import tn.esprit.spring.communicationservice.staffmessaging.websocket.StaffMessagingRealtimeNotifier;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalStaffMessagingServiceImpl implements InternalStaffMessagingService {

    private static final Set<InternalStaffRole> ALLOWED_MESSAGING_ROLES = EnumSet.allOf(InternalStaffRole.class);
    private static final int DEFAULT_USER_LIMIT = 20;
    private static final int MAX_USER_LIMIT = 50;

    private final StaffConversationRepository conversationRepository;
    private final StaffConversationParticipantRepository participantRepository;
    private final StaffMessageRepository messageRepository;
    private final UserDirectoryClient userDirectoryClient;
    private final CurrentUserService currentUserService;
    private final StaffMessagingMapper mapper;
    private final StaffMessagingRealtimeNotifier realtimeNotifier;

    @Override
    @Transactional(readOnly = true)
    public List<StaffMessagingUserResponse> listAvailableStaffUsers(String query, int limit) {
        CurrentStaffProfile currentUser = getCurrentStaffProfile();
        int safeLimit = normalizeLimit(limit);

        return userDirectoryClient.searchStaffUsers(trimToNull(query), List.copyOf(ALLOWED_MESSAGING_ROLES), safeLimit + 1).stream()
                .filter(UserSummary::isEnabled)
                .filter(user -> InternalStaffRole.fromValue(user.getRole()) != null)
                .filter(user -> !equalsIgnoreCase(user.getKeycloakId(), currentUser.userId()))
                .sorted(Comparator.comparing(UserSummary::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .limit(safeLimit)
                .map(mapper::toUserResponse)
                .toList();
    }

    @Override
    public StaffConversationSummaryResponse createOrGetDirectConversation(CreateDirectStaffConversationRequest request) {
        CurrentStaffProfile currentUser = getCurrentStaffProfile();
        String targetUserId = trimToNull(request.getTargetUserId());
        if (targetUserId == null) {
            throw new BadRequestException("targetUserId is required");
        }
        if (equalsIgnoreCase(currentUser.userId(), targetUserId)) {
            throw new BadRequestException("You cannot create a direct conversation with yourself");
        }

        UserSummary targetUser = userDirectoryClient.resolveStaffUser(targetUserId, ALLOWED_MESSAGING_ROLES);
        String directConversationKey = buildDirectConversationKey(currentUser.userId(), targetUser.getKeycloakId());

        StaffConversation conversation = conversationRepository.findByDirectConversationKey(directConversationKey)
                .orElseGet(() -> createConversation(currentUser, targetUser, directConversationKey));

        StaffConversationParticipant currentParticipant = participantRepository.findByConversation_IdAndUserId(conversation.getId(), currentUser.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Current staff participant is missing from conversation"));

        if (currentParticipant.isArchived()) {
            currentParticipant.setArchived(false);
            participantRepository.save(currentParticipant);
        }

        return buildConversationSummary(conversation, currentParticipant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffConversationSummaryResponse> listMyConversations() {
        String currentUserId = getCurrentStaffProfile().userId();
        return participantRepository.findActiveByUserIdOrderByRecentActivity(currentUserId).stream()
                .map(participant -> buildConversationSummary(participant.getConversation(), participant))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffMessageResponse> listConversationMessages(UUID conversationId) {
        StaffConversationParticipant participant = requireParticipant(conversationId, getCurrentStaffProfile().userId());
        return messageRepository.findByConversation_IdAndDeletedAtIsNullOrderByCreatedAtAsc(participant.getConversation().getId()).stream()
                .map(mapper::toMessageResponse)
                .toList();
    }

    @Override
    public StaffMessageResponse sendMessage(UUID conversationId, SendStaffMessageRequest request) {
        CurrentStaffProfile currentUser = getCurrentStaffProfile();
        StaffConversationParticipant participant = requireParticipant(conversationId, currentUser.userId());
        String content = trimToNull(request.getContent());
        if (content == null) {
            throw new BadRequestException("content is required");
        }

        Instant now = Instant.now();
        StaffMessage message = new StaffMessage();
        message.setConversation(participant.getConversation());
        message.setSenderId(currentUser.userId());
        message.setSenderRole(currentUser.role());
        message.setSenderDisplayName(currentUser.displayName());
        message.setContent(content);
        message.setMessageType(StaffMessageType.TEXT);
        message.setCreatedAt(now);
        StaffMessage savedMessage = messageRepository.save(message);

        StaffConversation conversation = participant.getConversation();
        conversation.setUpdatedAt(now);
        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);

        participant.setLastReadAt(now);
        participant.setArchived(false);
        participantRepository.save(participant);

        realtimeNotifier.notifyMessageCreated(conversation.getId(), savedMessage.getId());
        return mapper.toMessageResponse(savedMessage);
    }

    @Override
    public StaffConversationSummaryResponse markConversationRead(UUID conversationId) {
        CurrentStaffProfile currentUser = getCurrentStaffProfile();
        StaffConversationParticipant participant = requireParticipant(conversationId, currentUser.userId());
        participant.setLastReadAt(Instant.now());
        participant.setArchived(false);
        StaffConversationParticipant savedParticipant = participantRepository.save(participant);

        realtimeNotifier.notifyConversationRead(conversationId, currentUser.userId());
        return buildConversationSummary(savedParticipant.getConversation(), savedParticipant);
    }

    private StaffConversation createConversation(CurrentStaffProfile currentUser, UserSummary targetUser, String directConversationKey) {
        Instant now = Instant.now();

        StaffConversation conversation = new StaffConversation();
        conversation.setType(StaffConversationType.DIRECT);
        conversation.setDirectConversationKey(directConversationKey);
        conversation.setCreatedByUserId(currentUser.userId());
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        StaffConversation savedConversation = conversationRepository.save(conversation);

        participantRepository.save(newParticipant(savedConversation, currentUser.userId(), currentUser.role(), currentUser.displayName(), now));
        participantRepository.save(newParticipant(
                savedConversation,
                targetUser.getKeycloakId(),
                InternalStaffRole.fromValue(targetUser.getRole()),
                targetUser.getDisplayName(),
                now
        ));

        return savedConversation;
    }

    private StaffConversationParticipant newParticipant(
            StaffConversation conversation,
            String userId,
            InternalStaffRole role,
            String displayName,
            Instant joinedAt
    ) {
        StaffConversationParticipant participant = new StaffConversationParticipant();
        participant.setConversation(conversation);
        participant.setUserId(userId);
        participant.setUserRole(role);
        participant.setDisplayName(displayName);
        participant.setJoinedAt(joinedAt);
        participant.setArchived(false);
        participant.setMuted(false);
        return participant;
    }

    private StaffConversationParticipant requireParticipant(UUID conversationId, String userId) {
        return participantRepository.findByConversation_IdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in this conversation"));
    }

    private StaffConversationSummaryResponse buildConversationSummary(StaffConversation conversation, StaffConversationParticipant currentParticipant) {
        List<StaffConversationParticipant> participants = participantRepository.findByConversation_IdOrderByJoinedAtAsc(conversation.getId());
        StaffMessage lastMessage = messageRepository.findTopByConversation_IdAndDeletedAtIsNullOrderByCreatedAtDesc(conversation.getId()).orElse(null);
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

    private CurrentStaffProfile getCurrentStaffProfile() {
        return new CurrentStaffProfile(
                currentUserService.getCurrentUserSub(),
                currentUserService.getInternalStaffMessagingRoleOrThrow(),
                currentUserService.getDisplayNameOrUsername()
        );
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_USER_LIMIT;
        }
        return Math.min(limit, MAX_USER_LIMIT);
    }

    private String buildDirectConversationKey(String leftUserId, String rightUserId) {
        String normalizedLeft = normalizeKeyPart(leftUserId);
        String normalizedRight = normalizeKeyPart(rightUserId);
        return normalizedLeft.compareTo(normalizedRight) <= 0
                ? normalizedLeft + "::" + normalizedRight
                : normalizedRight + "::" + normalizedLeft;
    }

    private String normalizeKeyPart(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BadRequestException("Conversation participant id is required");
        }
        return normalized.toLowerCase();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record CurrentStaffProfile(String userId, InternalStaffRole role, String displayName) {
    }
}
