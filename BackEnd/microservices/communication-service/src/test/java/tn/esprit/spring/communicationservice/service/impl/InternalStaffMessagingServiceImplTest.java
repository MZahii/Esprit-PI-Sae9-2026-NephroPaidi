package tn.esprit.spring.communicationservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.spring.communicationservice.integration.UserDirectoryClient;
import tn.esprit.spring.communicationservice.integration.dto.UserSummary;
import tn.esprit.spring.communicationservice.security.CurrentUserService;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;
import tn.esprit.spring.communicationservice.staffmessaging.domain.StaffConversationType;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffConversation;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffConversationParticipant;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffMessage;
import tn.esprit.spring.communicationservice.staffmessaging.dto.request.CreateDirectStaffConversationRequest;
import tn.esprit.spring.communicationservice.staffmessaging.dto.request.SendStaffMessageRequest;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffConversationSummaryResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessageResponse;
import tn.esprit.spring.communicationservice.staffmessaging.repository.StaffConversationParticipantRepository;
import tn.esprit.spring.communicationservice.staffmessaging.repository.StaffConversationRepository;
import tn.esprit.spring.communicationservice.staffmessaging.repository.StaffMessageRepository;
import tn.esprit.spring.communicationservice.staffmessaging.service.StaffMessagingMapper;
import tn.esprit.spring.communicationservice.staffmessaging.service.impl.InternalStaffMessagingServiceImpl;
import tn.esprit.spring.communicationservice.staffmessaging.websocket.StaffMessagingRealtimeNotifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InternalStaffMessagingServiceImpl - Staff Messenger Flows")
class InternalStaffMessagingServiceImplTest {

    private static final String CURRENT_USER_ID = "doctor-1";

    @Mock
    private StaffConversationRepository conversationRepository;

    @Mock
    private StaffConversationParticipantRepository participantRepository;

    @Mock
    private StaffMessageRepository messageRepository;

    @Mock
    private UserDirectoryClient userDirectoryClient;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private StaffMessagingRealtimeNotifier realtimeNotifier;

    private final StaffMessagingMapper mapper = new StaffMessagingMapper();

    private InternalStaffMessagingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InternalStaffMessagingServiceImpl(
                conversationRepository,
                participantRepository,
                messageRepository,
                userDirectoryClient,
                currentUserService,
                mapper,
                realtimeNotifier
        );

        lenient().when(currentUserService.getCurrentUserSub()).thenReturn(CURRENT_USER_ID);
        lenient().when(currentUserService.getInternalStaffMessagingRoleOrThrow()).thenReturn(InternalStaffRole.DOCTOR);
        lenient().when(currentUserService.getDisplayNameOrUsername()).thenReturn("Doctor One");
    }

    @Test
    @DisplayName("listAvailableStaffUsers filters out current user")
    void listAvailableStaffUsersFiltersOutCurrentUser() {
        UserSummary currentUser = user("doctor-1", "doctor.one", "Doctor One", "DOCTOR");
        UserSummary nurse = user("nurse-2", "nurse.two", "Nurse Two", "NURSE");

        when(userDirectoryClient.searchStaffUsers(any(), anyList(), anyInt())).thenReturn(List.of(currentUser, nurse));

        List<?> users = service.listAvailableStaffUsers("nu", 10);

        assertEquals(1, users.size());
    }

    @Test
    @DisplayName("createOrGetDirectConversation creates a new direct conversation")
    void createOrGetDirectConversationCreatesNewConversation() {
        CreateDirectStaffConversationRequest request = new CreateDirectStaffConversationRequest();
        request.setTargetUserId("nurse-2");

        UserSummary targetUser = user("nurse-2", "nurse.two", "Nurse Two", "NURSE");
        StaffConversation savedConversation = new StaffConversation();
        savedConversation.setId(UUID.randomUUID());
        savedConversation.setType(StaffConversationType.DIRECT);
        savedConversation.setCreatedByUserId(CURRENT_USER_ID);
        savedConversation.setCreatedAt(Instant.now());
        savedConversation.setUpdatedAt(savedConversation.getCreatedAt());

        StaffConversationParticipant currentParticipant = participant(savedConversation, CURRENT_USER_ID, InternalStaffRole.DOCTOR, "Doctor One");
        StaffConversationParticipant targetParticipant = participant(savedConversation, "nurse-2", InternalStaffRole.NURSE, "Nurse Two");

        when(userDirectoryClient.resolveStaffUser(any(), any())).thenReturn(targetUser);
        when(conversationRepository.findByDirectConversationKey(any())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(StaffConversation.class))).thenReturn(savedConversation);
        when(participantRepository.findByConversation_IdAndUserId(savedConversation.getId(), CURRENT_USER_ID)).thenReturn(Optional.of(currentParticipant));
        when(participantRepository.findByConversation_IdOrderByJoinedAtAsc(savedConversation.getId())).thenReturn(List.of(currentParticipant, targetParticipant));
        when(messageRepository.findTopByConversation_IdAndDeletedAtIsNullOrderByCreatedAtDesc(savedConversation.getId())).thenReturn(Optional.empty());
        when(messageRepository.countByConversation_IdAndSenderIdNotAndDeletedAtIsNull(savedConversation.getId(), CURRENT_USER_ID)).thenReturn(0L);

        StaffConversationSummaryResponse response = service.createOrGetDirectConversation(request);

        assertEquals(savedConversation.getId(), response.getId());
        assertEquals(2, response.getParticipants().size());
        verify(participantRepository, times(2)).save(any(StaffConversationParticipant.class));
    }

    @Test
    @DisplayName("sendMessage persists the message and triggers realtime delivery")
    void sendMessagePersistsMessageAndNotifies() {
        UUID conversationId = UUID.randomUUID();
        StaffConversation conversation = new StaffConversation();
        conversation.setId(conversationId);
        conversation.setType(StaffConversationType.DIRECT);
        conversation.setCreatedByUserId("nurse-2");
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(conversation.getCreatedAt());

        StaffConversationParticipant currentParticipant = participant(conversation, CURRENT_USER_ID, InternalStaffRole.DOCTOR, "Doctor One");

        when(participantRepository.findByConversation_IdAndUserId(conversationId, CURRENT_USER_ID)).thenReturn(Optional.of(currentParticipant));
        when(messageRepository.save(any(StaffMessage.class))).thenAnswer(invocation -> {
            StaffMessage message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            return message;
        });

        SendStaffMessageRequest request = new SendStaffMessageRequest();
        request.setContent("Please review the chart.");

        StaffMessageResponse response = service.sendMessage(conversationId, request);

        assertEquals("Please review the chart.", response.getContent());
        verify(realtimeNotifier).notifyMessageCreated(any(), any());
    }

    @Test
    @DisplayName("listConversationMessages rejects non participants")
    void listConversationMessagesRejectsNonParticipants() {
        UUID conversationId = UUID.randomUUID();
        when(participantRepository.findByConversation_IdAndUserId(conversationId, CURRENT_USER_ID)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.listConversationMessages(conversationId));
        verify(messageRepository, never()).findByConversation_IdAndDeletedAtIsNullOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("listAvailableStaffUsers rejects non-staff roles")
    void listAvailableStaffUsersRejectsNonStaffRoles() {
        when(currentUserService.getInternalStaffMessagingRoleOrThrow()).thenThrow(new AccessDeniedException("forbidden"));

        assertThrows(AccessDeniedException.class, () -> service.listAvailableStaffUsers(null, 10));
        verify(userDirectoryClient, never()).searchStaffUsers(any(), anyList(), anyInt());
    }

    private UserSummary user(String keycloakId, String username, String displayName, String role) {
        UserSummary summary = new UserSummary();
        summary.setKeycloakId(keycloakId);
        summary.setUsername(username);
        summary.setRole(role);
        summary.setEnabled(true);
        String[] nameParts = displayName.split(" ", 2);
        summary.setFirstName(nameParts[0]);
        summary.setLastName(nameParts.length > 1 ? nameParts[1] : null);
        return summary;
    }

    private StaffConversationParticipant participant(
            StaffConversation conversation,
            String userId,
            InternalStaffRole role,
            String displayName
    ) {
        StaffConversationParticipant participant = new StaffConversationParticipant();
        participant.setConversation(conversation);
        participant.setUserId(userId);
        participant.setUserRole(role);
        participant.setDisplayName(displayName);
        participant.setJoinedAt(Instant.now());
        participant.setArchived(false);
        participant.setMuted(false);
        return participant;
    }
}
