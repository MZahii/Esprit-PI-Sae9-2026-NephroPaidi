package tn.esprit.spring.communicationservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.spring.communicationservice.domain.entity.AppointmentRequest;
import tn.esprit.spring.communicationservice.domain.enums.AppointmentStatus;
import tn.esprit.spring.communicationservice.dto.request.ApproveAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.request.CreateAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.response.AppointmentRequestResponse;
import tn.esprit.spring.communicationservice.exception.BadRequestException;
import tn.esprit.spring.communicationservice.mapper.AppointmentMapper;
import tn.esprit.spring.communicationservice.repository.AppointmentRequestRepository;
import tn.esprit.spring.communicationservice.security.CurrentUserService;
import tn.esprit.spring.communicationservice.service.GuardianPatientResolverService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentRequestServiceImplTest {

    @Mock
    private AppointmentRequestRepository appointmentRequestRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private GuardianPatientResolverService guardianPatientResolverService;

    @InjectMocks
    private AppointmentRequestServiceImpl service;

    @Test
    void create_shouldPersistTrimmedReasonAndGuardianContext() {
        LocalDateTime requestedDate = LocalDateTime.now().plusDays(2);
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setPatientId(55L);
        request.setRequestedDate(requestedDate);
        request.setReason("  controle nephro  ");

        when(guardianPatientResolverService.resolvePatientIdForMessage(55L)).thenReturn(55L);
        when(currentUserService.getCurrentUserSub()).thenReturn("guardian-sub");
        when(appointmentRequestRepository.save(any(AppointmentRequest.class))).thenAnswer(invocation -> {
            AppointmentRequest saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AppointmentRequestResponse mapped = AppointmentRequestResponse.builder()
                .id(UUID.randomUUID())
                .status(AppointmentStatus.REQUESTED)
                .reason("controle nephro")
                .build();
        when(appointmentMapper.toResponse(any(AppointmentRequest.class))).thenReturn(mapped);

        AppointmentRequestResponse response = service.create(request);

        verify(currentUserService).requireRole("GUARDIAN");
        verify(guardianPatientResolverService).resolvePatientIdForMessage(55L);

        ArgumentCaptor<AppointmentRequest> captor = ArgumentCaptor.forClass(AppointmentRequest.class);
        verify(appointmentRequestRepository).save(captor.capture());
        AppointmentRequest persisted = captor.getValue();
        assertEquals("controle nephro", persisted.getReason());
        assertEquals("guardian-sub", persisted.getGuardianKeycloakId());
        assertEquals(AppointmentStatus.REQUESTED, persisted.getStatus());
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());

        assertEquals(AppointmentStatus.REQUESTED, response.getStatus());
    }

    @Test
    void create_shouldRejectPastRequestedDate() {
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setPatientId(7L);
        request.setRequestedDate(LocalDateTime.now().minusMinutes(1));
        request.setReason("test");

        when(guardianPatientResolverService.resolvePatientIdForMessage(7L)).thenReturn(7L);

        assertThrows(BadRequestException.class, () -> service.create(request));

        verify(appointmentRequestRepository, never()).save(any());
        verify(appointmentMapper, never()).toResponse(any());
    }

    @Test
    void approve_shouldRejectWhenStatusIsNotRequested() {
        UUID id = UUID.randomUUID();
        AppointmentRequest entity = baseEntity(AppointmentStatus.CANCELLED, "guardian-sub");
        entity.setId(id);
        when(appointmentRequestRepository.findById(id)).thenReturn(Optional.of(entity));

        ApproveAppointmentRequest request = new ApproveAppointmentRequest();
        request.setScheduledDate(LocalDateTime.now().plusDays(1));

        assertThrows(BadRequestException.class, () -> service.approve(id, request));
        verify(appointmentRequestRepository, never()).save(any());
    }

    @Test
    void approve_shouldTrimReceptionistNotesAndSetApproved() {
        UUID id = UUID.randomUUID();
        AppointmentRequest entity = baseEntity(AppointmentStatus.REQUESTED, "guardian-sub");
        entity.setId(id);
        when(appointmentRequestRepository.findById(id)).thenReturn(Optional.of(entity));
        when(appointmentRequestRepository.save(any(AppointmentRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentMapper.toResponse(any(AppointmentRequest.class))).thenReturn(
                AppointmentRequestResponse.builder().id(id).status(AppointmentStatus.APPROVED).build()
        );

        ApproveAppointmentRequest request = new ApproveAppointmentRequest();
        request.setScheduledDate(LocalDateTime.now().plusDays(1));
        request.setReceptionistNotes("   note reception   ");

        AppointmentRequestResponse response = service.approve(id, request);

        ArgumentCaptor<AppointmentRequest> captor = ArgumentCaptor.forClass(AppointmentRequest.class);
        verify(appointmentRequestRepository).save(captor.capture());
        AppointmentRequest updated = captor.getValue();
        assertEquals(AppointmentStatus.APPROVED, updated.getStatus());
        assertEquals("note reception", updated.getReceptionistNotes());
        assertNotNull(updated.getUpdatedAt());
        assertEquals(AppointmentStatus.APPROVED, response.getStatus());
    }

    @Test
    void cancel_shouldRejectWhenGuardianDoesNotOwnRequest() {
        UUID id = UUID.randomUUID();
        AppointmentRequest entity = baseEntity(AppointmentStatus.REQUESTED, "owner-sub");
        entity.setId(id);
        when(appointmentRequestRepository.findById(id)).thenReturn(Optional.of(entity));
        when(currentUserService.getCurrentUserSub()).thenReturn("another-sub");

        assertThrows(AccessDeniedException.class, () -> service.cancel(id));
        verify(appointmentRequestRepository, never()).save(any());
    }

    @Test
    void listRequests_shouldFilterByStatus() {
        AppointmentRequest requested = baseEntity(AppointmentStatus.REQUESTED, "g1");
        AppointmentRequest approved = baseEntity(AppointmentStatus.APPROVED, "g1");
        when(appointmentRequestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(requested, approved));
        when(appointmentMapper.toResponse(eq(approved))).thenReturn(
                AppointmentRequestResponse.builder().status(AppointmentStatus.APPROVED).build()
        );

        List<AppointmentRequestResponse> result = service.listRequests(AppointmentStatus.APPROVED);

        verify(currentUserService).requireRole("RECEPTIONIST");
        assertEquals(1, result.size());
        assertEquals(AppointmentStatus.APPROVED, result.get(0).getStatus());
    }

    private AppointmentRequest baseEntity(AppointmentStatus status, String guardianSub) {
        AppointmentRequest entity = new AppointmentRequest();
        entity.setPatientId(10L);
        entity.setGuardianKeycloakId(guardianSub);
        entity.setReason("reason");
        entity.setStatus(status);
        entity.setReceptionistNotes(null);
        entity.setScheduledDate(null);
        entity.setCreatedAt(Instant.now().minusSeconds(60));
        entity.setUpdatedAt(Instant.now().minusSeconds(30));
        return entity;
    }
}
