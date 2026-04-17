package tn.esprit.spring.opsservice.hospitalization.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.opsservice.hospitalization.api.dto.CreateHospitalizationRequest;
import tn.esprit.spring.opsservice.hospitalization.api.dto.CreateHospitalizationTaskRequest;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationCaseResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationSummaryResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskUpdateRequest;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationCase;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationMeasurementKind;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationStatus;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTask;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskExecution;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskStatus;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskType;
import tn.esprit.spring.opsservice.hospitalization.repository.HospitalizationCaseRepository;
import tn.esprit.spring.opsservice.hospitalization.repository.HospitalizationTaskExecutionRepository;
import tn.esprit.spring.opsservice.hospitalization.repository.HospitalizationTaskRepository;
import tn.esprit.spring.opsservice.security.CurrentUserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HospitalizationServiceImpl - Core Flows")
class HospitalizationServiceImplTest {

    @Mock
    private HospitalizationCaseRepository hospitalizationCaseRepository;

    @Mock
    private HospitalizationTaskRepository hospitalizationTaskRepository;

    @Mock
    private HospitalizationTaskExecutionRepository hospitalizationTaskExecutionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private HospitalizationServiceImpl service;

    private CurrentUserService.AuthenticatedUser doctor;

    @BeforeEach
    void setUp() {
        doctor = new CurrentUserService.AuthenticatedUser("doctor-sub-1", "dr.haddad");
    }

    @Test
    @DisplayName("createHospitalization stores an empty case as requested")
    void createHospitalizationWithNoTasksStartsRequested() {
        CreateHospitalizationRequest request = new CreateHospitalizationRequest(
                10L,
                UUID.randomUUID(),
                "Observation after consultation",
                null
        );
        when(currentUserService.getCurrentUser()).thenReturn(doctor);
        when(hospitalizationCaseRepository.save(any(HospitalizationCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HospitalizationCaseResponse response = service.createHospitalization(request);

        assertEquals(10L, response.patientId());
        assertEquals(HospitalizationStatus.REQUESTED, response.status());
        assertEquals(0, response.tasks().size());
        verify(hospitalizationCaseRepository).save(any(HospitalizationCase.class));
    }

    @Test
    @DisplayName("createHospitalization with tasks activates the case and preserves task order")
    void createHospitalizationWithTasksActivatesCase() {
        CreateHospitalizationTaskRequest taskA = new CreateHospitalizationTaskRequest(
            HospitalizationTaskType.BLOOD_MONITORING,
                "Check vitals",
                "Measure blood pressure",
                HospitalizationMeasurementKind.NUMERIC,
                "mmHg",
                null
        );
        CreateHospitalizationTaskRequest taskB = new CreateHospitalizationTaskRequest(
            HospitalizationTaskType.PATIENT_MONITORING,
                "Hydration",
                "Track fluids",
                HospitalizationMeasurementKind.TEXT,
                null,
                5
        );
        CreateHospitalizationRequest request = new CreateHospitalizationRequest(
                12L,
                UUID.randomUUID(),
                "Need monitoring",
                List.of(taskA, taskB)
        );
        when(currentUserService.getCurrentUser()).thenReturn(doctor);
        when(hospitalizationCaseRepository.save(any(HospitalizationCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HospitalizationCaseResponse response = service.createHospitalization(request);

        assertEquals(HospitalizationStatus.ACTIVE, response.status());
        assertEquals(2, response.tasks().size());
        assertEquals(0, response.tasks().get(0).displayOrder());
        assertEquals(5, response.tasks().get(1).displayOrder());
    }

    @Test
    @DisplayName("addTask promotes requested hospitalization to active")
    void addTaskPromotesRequestedHospitalization() {
        UUID hospitalizationId = UUID.randomUUID();
        HospitalizationCase hospitalizationCase = hospitalizationCase(hospitalizationId, HospitalizationStatus.REQUESTED);
        HospitalizationTask existingTask = task(hospitalizationCase, "Existing", 2, HospitalizationMeasurementKind.NONE);
        hospitalizationCase.getTasks().add(existingTask);
        when(hospitalizationCaseRepository.findDetailedById(hospitalizationId)).thenReturn(java.util.Optional.of(hospitalizationCase));
        when(hospitalizationCaseRepository.save(any(HospitalizationCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HospitalizationTaskResponse response = service.addTask(hospitalizationId, new CreateHospitalizationTaskRequest(
            HospitalizationTaskType.PATIENT_MONITORING,
                "New task",
                "Do something",
                HospitalizationMeasurementKind.TEXT,
                null,
                null
        ));

        assertEquals(HospitalizationStatus.ACTIVE, hospitalizationCase.getStatus());
        assertEquals(3, response.displayOrder());
        assertEquals("New task", response.title());
    }

    @Test
    @DisplayName("addTask rejects closed hospitalization")
    void addTaskRejectsClosedHospitalization() {
        UUID hospitalizationId = UUID.randomUUID();
        HospitalizationCase hospitalizationCase = hospitalizationCase(hospitalizationId, HospitalizationStatus.COMPLETED);
        when(hospitalizationCaseRepository.findDetailedById(hospitalizationId)).thenReturn(java.util.Optional.of(hospitalizationCase));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.addTask(
                hospitalizationId,
                new CreateHospitalizationTaskRequest(
                    HospitalizationTaskType.PATIENT_MONITORING,
                        "New task",
                        null,
                        HospitalizationMeasurementKind.TEXT,
                        null,
                        null
                )
        ));

        assertEquals(409, exception.getStatusCode().value());
        verify(hospitalizationCaseRepository, never()).save(any(HospitalizationCase.class));
    }

    @Test
    @DisplayName("recordTaskExecution stores numeric measurements and updates task state")
    void recordTaskExecutionStoresMeasurement() {
        UUID hospitalizationId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        HospitalizationCase hospitalizationCase = hospitalizationCase(hospitalizationId, HospitalizationStatus.ACTIVE);
        HospitalizationTask task = task(hospitalizationCase, "Vitals", 0, HospitalizationMeasurementKind.NUMERIC);
        task.setId(taskId);
        hospitalizationCase.getTasks().add(task);

        when(hospitalizationTaskRepository.findById(taskId)).thenReturn(java.util.Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUserService.AuthenticatedUser("nurse-sub-9", "nurse.amina"));
        when(hospitalizationTaskExecutionRepository.save(any(HospitalizationTaskExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hospitalizationTaskRepository.save(any(HospitalizationTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HospitalizationTaskResponse response = service.recordTaskExecution(taskId, new HospitalizationTaskUpdateRequest(
                HospitalizationTaskStatus.DONE,
                "Completed",
                new BigDecimal("120"),
                null,
                null
        ));

        assertEquals(HospitalizationTaskStatus.DONE, response.status());
        assertEquals(new BigDecimal("120"), response.latestNumericValue());
        assertEquals("mmHg", response.latestUnit());
        assertEquals("nurse.amina", response.lastUpdatedByNurseUsername());
        verify(hospitalizationTaskExecutionRepository).save(any(HospitalizationTaskExecution.class));
    }

    @Test
    @DisplayName("recordTaskExecution requires a numeric value for numeric tasks")
    void recordTaskExecutionRequiresNumericValue() {
        UUID hospitalizationId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        HospitalizationCase hospitalizationCase = hospitalizationCase(hospitalizationId, HospitalizationStatus.ACTIVE);
        HospitalizationTask task = task(hospitalizationCase, "Vitals", 0, HospitalizationMeasurementKind.NUMERIC);
        task.setId(taskId);
        hospitalizationCase.getTasks().add(task);

        when(hospitalizationTaskRepository.findById(taskId)).thenReturn(java.util.Optional.of(task));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.recordTaskExecution(taskId, new HospitalizationTaskUpdateRequest(
                HospitalizationTaskStatus.DONE,
                "Completed",
                null,
                null,
                null
        )));

        assertEquals(400, exception.getStatusCode().value());
        verify(hospitalizationTaskExecutionRepository, never()).save(any(HospitalizationTaskExecution.class));
    }

    @Test
    @DisplayName("getActiveHospitalizationsForNurse returns summary counts")
    void getActiveHospitalizationsForNurseReturnsSummaryCounts() {
        HospitalizationCase hospitalizationCase = hospitalizationCase(UUID.randomUUID(), HospitalizationStatus.ACTIVE);
        hospitalizationCase.setDoctorUsername(doctor.getUsername());
        hospitalizationCase.setReason("Monitoring");
        HospitalizationTask doneTask = task(hospitalizationCase, "Done", 0, HospitalizationMeasurementKind.NONE);
        doneTask.setStatus(HospitalizationTaskStatus.DONE);
        HospitalizationTask pendingTask = task(hospitalizationCase, "Pending", 1, HospitalizationMeasurementKind.NONE);
        hospitalizationCase.getTasks().add(doneTask);
        hospitalizationCase.getTasks().add(pendingTask);

        when(hospitalizationCaseRepository.findByStatusOrderByCreatedAtDesc(HospitalizationStatus.ACTIVE)).thenReturn(List.of(hospitalizationCase));

        List<HospitalizationSummaryResponse> response = service.getActiveHospitalizationsForNurse();

        assertEquals(1, response.size());
        assertEquals(2, response.get(0).totalTasks());
        assertEquals(1, response.get(0).completedTasks());
        assertEquals(1, response.get(0).pendingTasks());
    }

    private HospitalizationCase hospitalizationCase(UUID id, HospitalizationStatus status) {
        HospitalizationCase hospitalizationCase = HospitalizationCase.builder()
                .id(id)
                .patientId(33L)
                .consultationId(UUID.randomUUID())
                .doctorKeycloakId(doctor.getUserId())
                .doctorUsername(doctor.getUsername())
                .reason("Reason")
                .status(status)
                .build();
        hospitalizationCase.setTasks(new java.util.ArrayList<>());
        return hospitalizationCase;
    }

    private HospitalizationTask task(HospitalizationCase hospitalizationCase, String title, int order, HospitalizationMeasurementKind kind) {
        return HospitalizationTask.builder()
                .hospitalizationCase(hospitalizationCase)
            .type(HospitalizationTaskType.PATIENT_MONITORING)
                .title(title)
                .instructions("Instructions")
                .measurementKind(kind)
                .expectedUnit("mmHg")
                .displayOrder(order)
                .status(HospitalizationTaskStatus.PENDING)
                .build();
    }
}