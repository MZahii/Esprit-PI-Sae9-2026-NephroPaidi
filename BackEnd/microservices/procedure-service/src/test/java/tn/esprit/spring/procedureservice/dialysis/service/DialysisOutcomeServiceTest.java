package tn.esprit.spring.procedureservice.dialysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisOutcome;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisPlan;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisSession;
import tn.esprit.spring.procedureservice.dialysis.dto.request.ValidateDialysisOutcomeRequest;
import tn.esprit.spring.procedureservice.dialysis.repository.DialysisOutcomeRepository;
import tn.esprit.spring.procedureservice.shared.exception.BusinessException;

@DisplayName("DialysisOutcomeService Tests")
class DialysisOutcomeServiceTest {

    @Mock
    private DialysisOutcomeRepository repository;

    @Mock
    private DialysisSessionService sessionService;

    @InjectMocks
    private DialysisOutcomeService service;

    private DialysisSession activeSession;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        DialysisPlan activePlan = new DialysisPlan();
        activePlan.setId(10L);
        activePlan.setStatus("IN_PROGRESS");

        activeSession = new DialysisSession();
        activeSession.setId(20L);
        activeSession.setPlan(activePlan);
    }

    @Test
    @DisplayName("Create outcome should reject duplicate session outcome")
    void createForSessionShouldRejectDuplicate() {
        when(repository.existsBySessionId(20L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createForSession(20L));

        assertEquals("A dialysis outcome already exists for session 20.", ex.getMessage());
        verify(sessionService, never()).getById(any());
    }

    @Test
    @DisplayName("Create outcome should reject archived or cancelled plan")
    void createForSessionShouldRejectArchivedPlan() {
        DialysisPlan archivedPlan = new DialysisPlan();
        archivedPlan.setStatus("ARCHIVED");
        DialysisSession archivedSession = new DialysisSession();
        archivedSession.setId(21L);
        archivedSession.setPlan(archivedPlan);

        when(repository.existsBySessionId(21L)).thenReturn(false);
        when(sessionService.getById(21L)).thenReturn(archivedSession);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createForSession(21L));

        assertEquals("Cannot create an outcome for a ARCHIVED dialysis plan.", ex.getMessage());
        verify(repository, never()).save(any(DialysisOutcome.class));
    }

    @Test
    @DisplayName("Create outcome should save non validated outcome for active session")
    void createForSessionShouldSaveOutcome() {
        when(repository.existsBySessionId(20L)).thenReturn(false);
        when(sessionService.getById(20L)).thenReturn(activeSession);
        when(repository.save(any(DialysisOutcome.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DialysisOutcome result = service.createForSession(20L);

        assertEquals(activeSession, result.getSession());
        assertFalse(result.isValidated());
        verify(repository).save(any(DialysisOutcome.class));
    }

    @Test
    @DisplayName("Validate should require a meaningful summary when validated")
    void validateShouldRequireSummaryWhenValidated() {
        DialysisOutcome existing = new DialysisOutcome();
        existing.setId(30L);
        when(repository.findById(30L)).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.validate(30L, new ValidateDialysisOutcomeRequest(true, "ok")));

        assertEquals("Outcome summary must contain at least 5 characters when validated.", ex.getMessage());
        verify(repository, never()).save(any(DialysisOutcome.class));
    }

    @Test
    @DisplayName("Validate should trim and save summary")
    void validateShouldTrimAndSaveSummary() {
        DialysisOutcome existing = new DialysisOutcome();
        existing.setId(31L);
        when(repository.findById(31L)).thenReturn(Optional.of(existing));
        when(repository.save(any(DialysisOutcome.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DialysisOutcome result = service.validate(31L, new ValidateDialysisOutcomeRequest(true, "  stable child  "));

        assertEquals("stable child", result.getSummary());
        assertEquals(true, result.isValidated());
        verify(repository).save(existing);
    }
}
