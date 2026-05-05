package tn.esprit.spring.procedureservice.surgical.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.spring.procedureservice.notification.service.ResendEmailService;
import tn.esprit.spring.procedureservice.shared.exception.BusinessException;
import tn.esprit.spring.procedureservice.surgical.domain.entity.PreOpAssessment;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgicalCase;
import tn.esprit.spring.procedureservice.surgical.dto.request.CreateSurgicalCaseRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.DecideTransplantOfferRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateSurgicalCaseRequest;
import tn.esprit.spring.procedureservice.surgical.repository.PreOpAssessmentRepository;
import tn.esprit.spring.procedureservice.surgical.repository.SurgicalCaseRepository;

@DisplayName("SurgicalCaseService Tests")
class SurgicalCaseServiceTest {

    @Mock
    private SurgicalCaseRepository repository;

    @Mock
    private PreOpAssessmentRepository preOpAssessmentRepository;

    @Mock
    private ResendEmailService resendEmailService;

    @InjectMocks
    private SurgicalCaseService service;

    private SurgicalCase surgicalCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        surgicalCase = new SurgicalCase();
        surgicalCase.setId(1L);
        surgicalCase.setPatientId("15");
        surgicalCase.setFirstName("Sami");
        surgicalCase.setLastName("Kid");
        surgicalCase.setStatus("OPEN");
        surgicalCase.setOfferStatus("PENDING");
    }

    @Test
    @DisplayName("Create should always start in OPEN and PENDING")
    void createShouldForceDefaultStatuses() {
        CreateSurgicalCaseRequest request = new CreateSurgicalCaseRequest(
            "15",
            null,
            null,
            "Sami",
            "Kid",
            12,
            "MALE",
            "MRN-1",
            "Kidney Transplant Surgery",
            "Living Donor Kidney Transplant",
            "MAJOR_SURGERY",
            "SCHEDULED",
            "surgeon-1",
            null,
            null,
            null,
            LocalDate.of(2026, 4, 22),
            LocalTime.of(9, 0),
            180,
            "OR-1",
            "DONE"
        );
        when(repository.save(any(SurgicalCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SurgicalCase result = service.create(request);

        assertEquals("OPEN", result.getStatus());
        assertEquals("PENDING", result.getOfferStatus());
        verify(resendEmailService).sendSurgicalCaseCreatedNotification(any(SurgicalCase.class));
    }

    @Test
    @DisplayName("Update should reject invalid transition")
    void updateShouldRejectInvalidTransition() {
        surgicalCase.setStatus("DONE");
        when(repository.findById(1L)).thenReturn(Optional.of(surgicalCase));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.update(1L, new UpdateSurgicalCaseRequest("READY_FOR_INTERVENTION")));

        assertEquals("Invalid transition from DONE to READY_FOR_INTERVENTION.", ex.getMessage());
        verify(repository, never()).save(any(SurgicalCase.class));
    }

    @Test
    @DisplayName("Update should reject surgery start without eligible pre op")
    void updateShouldRejectInProgressWithoutEligiblePreOp() {
        when(repository.findById(1L)).thenReturn(Optional.of(surgicalCase));
        when(preOpAssessmentRepository.findTopBySurgicalCaseIdOrderByIdDesc(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.update(1L, new UpdateSurgicalCaseRequest("IN_PROGRESS")));

        assertEquals("Cannot start surgery: latest Pre-Op decision is BLOCKED.", ex.getMessage());
        verify(repository, never()).save(any(SurgicalCase.class));
    }

    @Test
    @DisplayName("Update should allow valid transition with eligible pre op")
    void updateShouldAllowValidTransition() {
        surgicalCase.setStatus("READY_FOR_INTERVENTION");
        PreOpAssessment latest = new PreOpAssessment();
        latest.setNotes("hemodynamicsOk=true;infectionScreenOk=true;anesthesiaClearanceOk=true;consentSigned=true");

        when(repository.findById(1L)).thenReturn(Optional.of(surgicalCase));
        when(preOpAssessmentRepository.findTopBySurgicalCaseIdOrderByIdDesc(1L)).thenReturn(Optional.of(latest));
        when(repository.save(any(SurgicalCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SurgicalCase result = service.update(1L, new UpdateSurgicalCaseRequest("IN_PROGRESS"));

        assertEquals("IN_PROGRESS", result.getStatus());
        verify(repository).save(surgicalCase);
    }

    @Test
    @DisplayName("Decide offer should reject invalid offer status")
    void decideOfferShouldRejectInvalidStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(surgicalCase));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.decideOffer(1L, new DecideTransplantOfferRequest("UNKNOWN")));

        assertEquals("Invalid transplant offer status: UNKNOWN", ex.getMessage());
        verify(repository, never()).save(any(SurgicalCase.class));
    }
}
