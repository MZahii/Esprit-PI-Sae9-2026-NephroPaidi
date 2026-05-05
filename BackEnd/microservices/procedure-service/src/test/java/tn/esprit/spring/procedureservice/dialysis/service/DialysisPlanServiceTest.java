package tn.esprit.spring.procedureservice.dialysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisPlan;
import tn.esprit.spring.procedureservice.dialysis.dto.request.CreateDialysisPlanRequest;
import tn.esprit.spring.procedureservice.dialysis.dto.request.UpdateDialysisPlanRequest;
import tn.esprit.spring.procedureservice.dialysis.repository.DialysisPlanRepository;
import tn.esprit.spring.procedureservice.shared.exception.BusinessException;

@DisplayName("DialysisPlanService Tests")
class DialysisPlanServiceTest {

    @Mock
    private DialysisPlanRepository repository;

    @InjectMocks
    private DialysisPlanService service;

    private CreateDialysisPlanRequest createRequest;
    private UpdateDialysisPlanRequest validUpdateRequest;
    private DialysisPlan existingPlan;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        createRequest = new CreateDialysisPlanRequest(
            "12",
            "Ali",
            "Boulifa",
            "doctor-1",
            "HEMODIALYSIS",
            3,
            240,
            LocalDate.of(2026, 4, 20),
            LocalDate.of(2026, 5, 20),
            "Monday, Wednesday, Friday",
            300,
            500,
            2,
            "CENTER-1",
            "R1",
            "M1",
            "ARCHIVED"
        );

        validUpdateRequest = new UpdateDialysisPlanRequest(
            "Ali",
            "Boulifa",
            "doctor-2",
            "HEMODIALYSIS",
            4,
            180,
            LocalDate.of(2026, 4, 21),
            LocalDate.of(2026, 5, 21),
            "Tuesday, Thursday, Saturday",
            320,
            520,
            3,
            "CENTER-2",
            "R2",
            "M2",
            "IN_PROGRESS"
        );

        existingPlan = new DialysisPlan();
        existingPlan.setId(1L);
        existingPlan.setPatientId("12");
        existingPlan.setFirstName("Ali");
        existingPlan.setLastName("Boulifa");
        existingPlan.setDoctorId("doctor-1");
        existingPlan.setDialysisType("HEMODIALYSIS");
        existingPlan.setSessionsPerWeek(3);
        existingPlan.setSessionDurationMinutes(240);
        existingPlan.setStartDate(LocalDate.of(2026, 4, 20));
        existingPlan.setEndDate(LocalDate.of(2026, 5, 20));
        existingPlan.setDaysOfWeek("Monday, Wednesday, Friday");
        existingPlan.setStatus("PLANNED");
    }

    @Test
    @DisplayName("Create should always start in PLANNED status")
    void createShouldForcePlannedStatus() {
        when(repository.save(any(DialysisPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DialysisPlan result = service.create(createRequest);

        assertEquals("PLANNED", result.getStatus());
        assertEquals("12", result.getPatientId());
        verify(repository).save(any(DialysisPlan.class));
    }

    @Test
    @DisplayName("Update should reject invalid status values")
    void updateShouldRejectInvalidStatus() {
        UpdateDialysisPlanRequest invalidRequest = new UpdateDialysisPlanRequest(
            "Ali",
            "Boulifa",
            "doctor-2",
            "HEMODIALYSIS",
            4,
            180,
            LocalDate.of(2026, 4, 21),
            LocalDate.of(2026, 5, 21),
            "Tuesday, Thursday, Saturday",
            320,
            520,
            3,
            "CENTER-2",
            "R2",
            "M2",
            "INVALID_STATUS"
        );
        when(repository.findById(1L)).thenReturn(Optional.of(existingPlan));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, invalidRequest));

        assertEquals("Invalid dialysis plan status: INVALID_STATUS", ex.getMessage());
        verify(repository, never()).save(any(DialysisPlan.class));
    }

    @Test
    @DisplayName("Update should reject invalid status transition")
    void updateShouldRejectInvalidTransition() {
        existingPlan.setStatus("COMPLETED");
        when(repository.findById(1L)).thenReturn(Optional.of(existingPlan));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, validUpdateRequest));

        assertEquals("Invalid transition from COMPLETED to IN_PROGRESS.", ex.getMessage());
        verify(repository, never()).save(any(DialysisPlan.class));
    }

    @Test
    @DisplayName("Update should persist all fields on valid transition")
    void updateShouldPersistAllFields() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingPlan));
        when(repository.save(any(DialysisPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DialysisPlan result = service.update(1L, validUpdateRequest);

        assertEquals("doctor-2", result.getDoctorId());
        assertEquals(4, result.getSessionsPerWeek());
        assertEquals(180, result.getSessionDurationMinutes());
        assertEquals("Tuesday, Thursday, Saturday", result.getDaysOfWeek());
        assertEquals("IN_PROGRESS", result.getStatus());
        verify(repository).save(existingPlan);
    }
}
