package tn.esprit.spring.communicationservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.communicationservice.domain.entity.QuickReplyTemplate;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.dto.request.CreateQuickReplyTemplateRequest;
import tn.esprit.spring.communicationservice.dto.request.UpdateQuickReplyTemplateRequest;
import tn.esprit.spring.communicationservice.dto.response.QuickReplyTemplateResponse;
import tn.esprit.spring.communicationservice.repository.QuickReplyTemplateRepository;
import tn.esprit.spring.communicationservice.security.CurrentUserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuickReplyTemplateServiceImpl - Template Flows")
class QuickReplyTemplateServiceImplTest {

    @Mock
    private QuickReplyTemplateRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private QuickReplyTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        when(currentUserService.getStaffRoleOrThrow()).thenReturn(tn.esprit.spring.communicationservice.domain.enums.StaffRole.RECEPTIONIST);
    }

    @Test
    @DisplayName("create trims content and initializes usage count")
    void createTrimsContentAndInitializesUsageCount() {
        CreateQuickReplyTemplateRequest request = new CreateQuickReplyTemplateRequest();
        request.setName("  Welcome  ");
        request.setMessageType(MessageType.APPOINTMENT);
        request.setTemplateText("  Your appointment is confirmed.  ");

        when(repository.save(any(QuickReplyTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuickReplyTemplateResponse response = service.create(request);

        assertEquals("Welcome", response.getName());
        assertEquals(0L, response.getUsageCount());
        assertEquals(MessageType.APPOINTMENT, response.getMessageType());
        verify(repository).save(any(QuickReplyTemplate.class));
    }

    @Test
    @DisplayName("list returns templates ordered by name")
    void listReturnsTemplates() {
        QuickReplyTemplate template = new QuickReplyTemplate();
        template.setId(UUID.randomUUID());
        template.setName("Alpha");
        template.setMessageType(MessageType.QUESTION);
        template.setTemplateText("Hello");
        template.setUsageCount(3);
        template.setCreatedAt(java.time.Instant.now());
        template.setUpdatedAt(java.time.Instant.now());

        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(template));

        List<QuickReplyTemplateResponse> responses = service.list(null);

        assertEquals(1, responses.size());
        assertEquals("Alpha", responses.get(0).getName());
        verify(currentUserService).getStaffRoleOrThrow();
    }

    @Test
    @DisplayName("update rejects missing templates")
    void updateRejectsMissingTemplates() {
        UpdateQuickReplyTemplateRequest request = new UpdateQuickReplyTemplateRequest();
        request.setName("Name");
        request.setMessageType(MessageType.APPOINTMENT);
        request.setTemplateText("Text");

        when(repository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.update(UUID.randomUUID(), request));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("incrementUsage increments usage count")
    void incrementUsageIncrementsUsageCount() {
        QuickReplyTemplate template = new QuickReplyTemplate();
        template.setId(UUID.randomUUID());
        template.setName("Alpha");
        template.setMessageType(MessageType.QUESTION);
        template.setTemplateText("Hello");
        template.setUsageCount(7);
        template.setCreatedAt(java.time.Instant.now());
        template.setUpdatedAt(java.time.Instant.now());

        when(repository.findById(template.getId())).thenReturn(Optional.of(template));
        when(repository.save(any(QuickReplyTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuickReplyTemplateResponse response = service.incrementUsage(template.getId());

        assertEquals(8L, response.getUsageCount());
    }
}