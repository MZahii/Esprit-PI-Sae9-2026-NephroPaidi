package tn.esprit.spring.communicationservice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionTemplate;
import tn.esprit.spring.communicationservice.domain.entity.FollowUpMessage;
import tn.esprit.spring.communicationservice.domain.entity.MessageAuditLog;
import tn.esprit.spring.communicationservice.domain.entity.MessageReply;
import tn.esprit.spring.communicationservice.domain.enums.MessageQueue;
import tn.esprit.spring.communicationservice.domain.enums.MessageStatus;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.domain.enums.PriorityLevel;
import tn.esprit.spring.communicationservice.domain.enums.SenderRole;
import tn.esprit.spring.communicationservice.domain.enums.StaffRole;
import tn.esprit.spring.communicationservice.dto.request.CreateMessageRequest;
import tn.esprit.spring.communicationservice.dto.request.EscalateRequest;
import tn.esprit.spring.communicationservice.dto.request.ReplyMessageRequest;
import tn.esprit.spring.communicationservice.dto.response.CreateMessageResponse;
import tn.esprit.spring.communicationservice.dto.response.FollowUpMessageResponse;
import tn.esprit.spring.communicationservice.mapper.MessageMapper;
import tn.esprit.spring.communicationservice.repository.FollowUpMessageRepository;
import tn.esprit.spring.communicationservice.repository.MessageAuditLogRepository;
import tn.esprit.spring.communicationservice.repository.MessageReplyRepository;
import tn.esprit.spring.communicationservice.security.CurrentUserService;
import tn.esprit.spring.communicationservice.service.GuardianPatientResolverService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpMessageServiceImpl - Message Flows")
class FollowUpMessageServiceImplTest {

    @Mock
    private FollowUpMessageRepository followUpMessageRepository;

    @Mock
    private MessageReplyRepository messageReplyRepository;

    @Mock
    private MessageAuditLogRepository messageAuditLogRepository;

    @Spy
    private MessageMapper messageMapper = new MessageMapper();

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private GuardianPatientResolverService guardianPatientResolverService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private FollowUpMessageServiceImpl service;

    @Test
    @DisplayName("create stores guardian message and audits it")
    void createStoresGuardianMessageAndAuditsIt() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setPatientId(15L);
        request.setMessageType(MessageType.APPOINTMENT);
        request.setPriority(PriorityLevel.HIGH);
        request.setSubject("  Follow-up  ");
        request.setMessageText("  Need a quick answer  ");

        when(currentUserService.getCurrentUserSub()).thenReturn("guardian-sub");
        when(currentUserService.getSenderRoleOrThrow()).thenReturn(SenderRole.GUARDIAN);
        doAnswer(invocation -> invocation.getArgument(0)).when(followUpMessageRepository).save(any(FollowUpMessage.class));
        when(guardianPatientResolverService.resolvePatientIdForMessage(15L)).thenReturn(45L);

        CreateMessageResponse response = service.create(request);

        assertEquals(MessageStatus.PENDING, response.getStatus());
        verify(messageAuditLogRepository, times(2)).save(any(MessageAuditLog.class));
        verify(followUpMessageRepository).save(any(FollowUpMessage.class));
    }

    @Test
    @DisplayName("take assigns message to current staff member")
    void takeAssignsMessageToCurrentStaffMember() {
        FollowUpMessage message = baseMessage(MessageQueue.NURSE, MessageStatus.PENDING);
        UUID id = message.getId();
        when(currentUserService.getStaffRoleOrThrow()).thenReturn(StaffRole.NURSE);
        when(currentUserService.getCurrentUserSub()).thenReturn("nurse-sub");
        when(followUpMessageRepository.findById(id)).thenReturn(Optional.of(message));
        when(messageReplyRepository.findByMessageIdOrderByCreatedAtAsc(id)).thenReturn(List.of());
        doAnswer(invocation -> invocation.getArgument(0)).when(followUpMessageRepository).save(any(FollowUpMessage.class));

        FollowUpMessageResponse response = service.take(id);

        assertEquals(MessageStatus.IN_PROGRESS, response.getStatus());
        assertEquals("nurse-sub", response.getAssignedToUserKeycloakId());
        verify(messageAuditLogRepository).save(any(MessageAuditLog.class));
    }

    @Test
    @DisplayName("reply updates message status for staff replies")
    void replyUpdatesMessageStatusForStaffReplies() {
        FollowUpMessage message = baseMessage(MessageQueue.DOCTOR, MessageStatus.IN_PROGRESS);
        UUID id = message.getId();
        message.setAssignedToUserKeycloakId("doctor-sub");
        when(followUpMessageRepository.findById(id)).thenReturn(Optional.of(message));
        when(currentUserService.isGuardian()).thenReturn(false);
        when(currentUserService.getStaffRoleOrThrow()).thenReturn(StaffRole.DOCTOR);
        when(currentUserService.getSenderRoleOrThrow()).thenReturn(SenderRole.DOCTOR);
        when(currentUserService.getCurrentUserSub()).thenReturn("doctor-sub");
        when(messageReplyRepository.findByMessageIdOrderByCreatedAtAsc(id)).thenReturn(List.of());
        doAnswer(invocation -> invocation.getArgument(0)).when(followUpMessageRepository).save(any(FollowUpMessage.class));

        ReplyMessageRequest request = new ReplyMessageRequest();
        request.setReplyText("  Please come tomorrow  ");

        FollowUpMessageResponse response = service.reply(id, request);

        assertEquals(MessageStatus.RESPONDED, response.getStatus());
        verify(messageAuditLogRepository).save(any(MessageAuditLog.class));
    }

    @Test
    @DisplayName("escalate moves message to doctor queue for nurses")
    void escalateMovesMessageToDoctorQueue() {
        FollowUpMessage message = baseMessage(MessageQueue.NURSE, MessageStatus.READ);
        UUID id = message.getId();
        when(currentUserService.getCurrentUserSub()).thenReturn("nurse-sub");
        when(followUpMessageRepository.findById(id)).thenReturn(Optional.of(message));
        when(messageReplyRepository.findByMessageIdOrderByCreatedAtAsc(id)).thenReturn(List.of());
        doAnswer(invocation -> invocation.getArgument(0)).when(followUpMessageRepository).save(any(FollowUpMessage.class));

        FollowUpMessageResponse response = service.escalate(id, new EscalateRequest() {{ setDoctorKeycloakId("doctor-target"); }});

        assertEquals(MessageQueue.DOCTOR, response.getQueue());
        assertEquals(MessageStatus.ESCALATED, response.getStatus());
        verify(messageAuditLogRepository).save(any(MessageAuditLog.class));
    }

    @Test
    @DisplayName("close rejects access for non owner guardians")
    void closeRejectsAccessForNonOwnerGuardians() {
        FollowUpMessage message = baseMessage(MessageQueue.RECEPTIONIST, MessageStatus.READ);
        UUID id = message.getId();
        when(followUpMessageRepository.findById(id)).thenReturn(Optional.of(message));
        when(currentUserService.isGuardian()).thenReturn(true);
        when(currentUserService.getCurrentUserSub()).thenReturn("other-guardian");

        assertThrows(AccessDeniedException.class, () -> service.close(id));
        verify(followUpMessageRepository, never()).save(any(FollowUpMessage.class));
    }

    private FollowUpMessage baseMessage(MessageQueue queue, MessageStatus status) {
        FollowUpMessage message = new FollowUpMessage();
        message.setId(UUID.randomUUID());
        message.setPatientId(12L);
        message.setGuardianKeycloakId("guardian-sub");
        message.setMessageType(MessageType.APPOINTMENT);
        message.setPriority(PriorityLevel.HIGH);
        message.setQueue(queue);
        message.setStatus(status);
        message.setSubject("Subject");
        message.setMessageText("Body");
        message.setCreatedAt(Instant.now().minusSeconds(30));
        message.setLastUpdatedAt(Instant.now().minusSeconds(10));
        return message;
    }

}