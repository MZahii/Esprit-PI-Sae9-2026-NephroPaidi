package tn.esprit.spring.opsservice.hospitalization.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.opsservice.hospitalization.api.dto.CreateHospitalizationRequest;
import tn.esprit.spring.opsservice.hospitalization.api.dto.CreateHospitalizationTaskRequest;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationCaseResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationSummaryResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskExecutionResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskUpdateRequest;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationCase;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationMeasurementKind;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationStatus;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTask;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskExecution;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskStatus;
import tn.esprit.spring.opsservice.hospitalization.repository.HospitalizationCaseRepository;
import tn.esprit.spring.opsservice.hospitalization.repository.HospitalizationTaskExecutionRepository;
import tn.esprit.spring.opsservice.hospitalization.repository.HospitalizationTaskRepository;
import tn.esprit.spring.opsservice.security.CurrentUserService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HospitalizationServiceImpl implements HospitalizationService {

    private final HospitalizationCaseRepository hospitalizationCaseRepository;
    private final HospitalizationTaskRepository hospitalizationTaskRepository;
    private final HospitalizationTaskExecutionRepository hospitalizationTaskExecutionRepository;
    private final CurrentUserService currentUserService;

    @Override
    public HospitalizationCaseResponse createHospitalization(CreateHospitalizationRequest request) {
        CurrentUserService.AuthenticatedUser doctor = currentUserService.getCurrentUser();

        HospitalizationCase hospitalizationCase = HospitalizationCase.builder()
                .patientId(request.patientId())
                .consultationId(request.consultationId())
                .doctorKeycloakId(doctor.getUserId())
                .doctorUsername(doctor.getUsername())
                .reason(request.reason())
                .status(request.safeTasks().isEmpty() ? HospitalizationStatus.REQUESTED : HospitalizationStatus.ACTIVE)
                .build();

        int fallbackOrder = 0;
        for (CreateHospitalizationTaskRequest taskRequest : request.safeTasks()) {
            hospitalizationCase.getTasks().add(toTaskEntity(hospitalizationCase, taskRequest, fallbackOrder++));
        }

        return toCaseResponse(hospitalizationCaseRepository.save(hospitalizationCase));
    }

    @Override
    public HospitalizationTaskResponse addTask(UUID hospitalizationId, CreateHospitalizationTaskRequest request) {
        HospitalizationCase hospitalizationCase = getExistingHospitalization(hospitalizationId);
        if (hospitalizationCase.getStatus() == HospitalizationStatus.CANCELLED
                || hospitalizationCase.getStatus() == HospitalizationStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot add tasks to a closed hospitalization");
        }

        int nextOrder = hospitalizationCase.getTasks().stream()
                .map(HospitalizationTask::getDisplayOrder)
                .max(Integer::compareTo)
                .orElse(-1) + 1;

        HospitalizationTask task = toTaskEntity(hospitalizationCase, request, nextOrder);
        hospitalizationCase.getTasks().add(task);
        if (hospitalizationCase.getStatus() == HospitalizationStatus.REQUESTED) {
            hospitalizationCase.setStatus(HospitalizationStatus.ACTIVE);
        }

        hospitalizationCaseRepository.save(hospitalizationCase);
        return toTaskResponse(task);
    }

    @Override
    @Transactional
    public HospitalizationCaseResponse getHospitalization(UUID hospitalizationId) {
        HospitalizationCase hospitalizationCase = hospitalizationCaseRepository.findDetailedById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalization not found"));
        return toCaseResponse(hospitalizationCase);
    }

    @Override
    public List<HospitalizationSummaryResponse> getActiveHospitalizationsForNurse() {
        return hospitalizationCaseRepository.findByStatusOrderByCreatedAtDesc(HospitalizationStatus.ACTIVE)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Override
    public HospitalizationTaskResponse recordTaskExecution(UUID taskId, HospitalizationTaskUpdateRequest request) {
        HospitalizationTask task = hospitalizationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        HospitalizationCase hospitalizationCase = task.getHospitalizationCase();
        if (hospitalizationCase.getStatus() != HospitalizationStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active hospitalizations can be updated");
        }

        validateMeasurement(task, request);

        CurrentUserService.AuthenticatedUser nurse = currentUserService.getCurrentUser();

        HospitalizationTaskExecution execution = HospitalizationTaskExecution.builder()
                .hospitalizationCase(hospitalizationCase)
                .task(task)
                .status(request.status())
                .nurseKeycloakId(nurse.getUserId())
                .nurseUsername(nurse.getUsername())
                .note(request.note())
                .numericValue(request.numericValue())
                .textValue(request.textValue())
                .unit(request.unit() != null ? request.unit() : task.getExpectedUnit())
                .build();
        hospitalizationTaskExecutionRepository.save(execution);

        task.setStatus(request.status());
        task.setLatestNote(request.note());
        task.setLatestNumericValue(request.numericValue());
        task.setLatestTextValue(request.textValue());
        task.setLatestUnit(request.unit() != null ? request.unit() : task.getExpectedUnit());
        task.setLastUpdatedByNurseId(nurse.getUserId());
        task.setLastUpdatedByNurseUsername(nurse.getUsername());
        task.setLastUpdatedAt(LocalDateTime.now());
        task.getExecutions().add(execution);

        hospitalizationTaskRepository.save(task);
        return toTaskResponse(task);
    }

    private HospitalizationCase getExistingHospitalization(UUID hospitalizationId) {
        return hospitalizationCaseRepository.findDetailedById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalization not found"));
    }

    private HospitalizationTask toTaskEntity(
            HospitalizationCase hospitalizationCase,
            CreateHospitalizationTaskRequest request,
            int fallbackOrder
    ) {
        return HospitalizationTask.builder()
                .hospitalizationCase(hospitalizationCase)
                .type(request.type())
                .title(request.title())
                .instructions(request.instructions())
                .measurementKind(request.measurementKind() == null ? HospitalizationMeasurementKind.NONE : request.measurementKind())
                .expectedUnit(request.expectedUnit())
                .displayOrder(request.displayOrder() == null ? fallbackOrder : request.displayOrder())
                .status(HospitalizationTaskStatus.PENDING)
                .build();
    }

    private void validateMeasurement(HospitalizationTask task, HospitalizationTaskUpdateRequest request) {
        HospitalizationMeasurementKind measurementKind = task.getMeasurementKind();
        if (measurementKind == HospitalizationMeasurementKind.NUMERIC && request.numericValue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This task requires a numeric value");
        }
        if (measurementKind == HospitalizationMeasurementKind.TEXT
                && (request.textValue() == null || request.textValue().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This task requires a text value");
        }
    }

    private HospitalizationSummaryResponse toSummaryResponse(HospitalizationCase hospitalizationCase) {
        int totalTasks = hospitalizationCase.getTasks().size();
        int completedTasks = (int) hospitalizationCase.getTasks().stream()
                .filter(task -> task.getStatus() == HospitalizationTaskStatus.DONE)
                .count();
        int pendingTasks = totalTasks - completedTasks;

        return new HospitalizationSummaryResponse(
                hospitalizationCase.getId(),
                hospitalizationCase.getPatientId(),
                hospitalizationCase.getConsultationId(),
                hospitalizationCase.getDoctorUsername(),
                hospitalizationCase.getReason(),
                hospitalizationCase.getStatus(),
                totalTasks,
                completedTasks,
                pendingTasks,
                hospitalizationCase.getCreatedAt(),
                hospitalizationCase.getUpdatedAt()
        );
    }

    private HospitalizationCaseResponse toCaseResponse(HospitalizationCase hospitalizationCase) {
        List<HospitalizationTaskResponse> tasks = hospitalizationCase.getTasks().stream()
                .sorted(Comparator.comparing(HospitalizationTask::getDisplayOrder)
                        .thenComparing(HospitalizationTask::getCreatedAt))
                .map(this::toTaskResponse)
                .toList();

        return new HospitalizationCaseResponse(
                hospitalizationCase.getId(),
                hospitalizationCase.getPatientId(),
                hospitalizationCase.getConsultationId(),
                hospitalizationCase.getDoctorKeycloakId(),
                hospitalizationCase.getDoctorUsername(),
                hospitalizationCase.getReason(),
                hospitalizationCase.getStatus(),
                hospitalizationCase.getCreatedAt(),
                hospitalizationCase.getUpdatedAt(),
                tasks
        );
    }

    private HospitalizationTaskResponse toTaskResponse(HospitalizationTask task) {
        List<HospitalizationTaskExecutionResponse> executions = task.getExecutions().stream()
                .map(execution -> new HospitalizationTaskExecutionResponse(
                        execution.getId(),
                        execution.getStatus(),
                        execution.getNurseKeycloakId(),
                        execution.getNurseUsername(),
                        execution.getNote(),
                        execution.getNumericValue(),
                        execution.getTextValue(),
                        execution.getUnit(),
                        execution.getRecordedAt()
                ))
                .toList();

        return new HospitalizationTaskResponse(
                task.getId(),
                task.getType(),
                task.getTitle(),
                task.getInstructions(),
                task.getMeasurementKind(),
                task.getExpectedUnit(),
                task.getDisplayOrder(),
                task.getStatus(),
                task.getLatestNote(),
                task.getLatestNumericValue(),
                task.getLatestTextValue(),
                task.getLatestUnit(),
                task.getLastUpdatedByNurseId(),
                task.getLastUpdatedByNurseUsername(),
                task.getLastUpdatedAt(),
                executions
        );
    }
}
