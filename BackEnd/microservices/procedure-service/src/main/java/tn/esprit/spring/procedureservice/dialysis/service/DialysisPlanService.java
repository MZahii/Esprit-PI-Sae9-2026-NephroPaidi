package tn.esprit.spring.procedureservice.dialysis.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisPlan;
import tn.esprit.spring.procedureservice.dialysis.dto.request.CreateDialysisPlanRequest;
import tn.esprit.spring.procedureservice.dialysis.dto.request.UpdateDialysisPlanRequest;
import tn.esprit.spring.procedureservice.dialysis.repository.DialysisPlanRepository;
import tn.esprit.spring.procedureservice.shared.exception.BusinessException;
import tn.esprit.spring.procedureservice.shared.exception.NotFoundException;

@Service
public class DialysisPlanService {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
        "PLANNED",
        "IN_PROGRESS",
        "COMPLETED",
        "CANCELLED",
        "ARCHIVED"
    );
    private static final Map<String, Set<String>> ALLOWED_STATUS_TRANSITIONS = Map.of(
        "PLANNED", Set.of("PLANNED", "IN_PROGRESS", "CANCELLED", "ARCHIVED"),
        "IN_PROGRESS", Set.of("IN_PROGRESS", "COMPLETED", "CANCELLED", "ARCHIVED"),
        "COMPLETED", Set.of("COMPLETED", "ARCHIVED"),
        "CANCELLED", Set.of("CANCELLED", "ARCHIVED"),
        "ARCHIVED", Set.of("ARCHIVED")
    );

    private final DialysisPlanRepository repository;

    public DialysisPlanService(DialysisPlanRepository repository) {
        this.repository = repository;
    }

    public DialysisPlan create(CreateDialysisPlanRequest request) {
        DialysisPlan plan = new DialysisPlan();
        plan.setPatientId(request.patientId());
        plan.setFirstName(request.firstName());
        plan.setLastName(request.lastName());
        plan.setDoctorId(request.doctorId());
        plan.setDialysisType(request.dialysisType());
        plan.setSessionsPerWeek(request.sessionsPerWeek());
        plan.setSessionDurationMinutes(request.sessionDurationMinutes());
        plan.setStartDate(request.startDate());
        plan.setEndDate(request.endDate());
        plan.setDaysOfWeek(request.daysOfWeek());
        plan.setBloodFlowRate(request.bloodFlowRate());
        plan.setDialysateFlowRate(request.dialysateFlowRate());
        plan.setUltrafiltrationGoal(request.ultrafiltrationGoal());
        plan.setDialysisCenterId(request.dialysisCenterId());
        plan.setRoomNumber(request.roomNumber());
        plan.setMachineId(request.machineId());
        plan.setStatus("PLANNED");
        return repository.save(plan);
    }

    public DialysisPlan update(Long id, UpdateDialysisPlanRequest request) {
        DialysisPlan plan = getById(id);
        String requestedStatus = normalizeStatus(request.status());
        if (!ALLOWED_STATUSES.contains(requestedStatus)) {
            throw new BusinessException("Invalid dialysis plan status: " + requestedStatus);
        }

        validateStatusTransition(plan.getStatus(), requestedStatus);

        plan.setFirstName(request.firstName());
        plan.setLastName(request.lastName());
        plan.setDoctorId(request.doctorId());
        plan.setDialysisType(request.dialysisType());
        plan.setSessionsPerWeek(request.sessionsPerWeek());
        plan.setSessionDurationMinutes(request.sessionDurationMinutes());
        plan.setStartDate(request.startDate());
        plan.setEndDate(request.endDate());
        plan.setDaysOfWeek(request.daysOfWeek());
        plan.setBloodFlowRate(request.bloodFlowRate());
        plan.setDialysateFlowRate(request.dialysateFlowRate());
        plan.setUltrafiltrationGoal(request.ultrafiltrationGoal());
        plan.setDialysisCenterId(request.dialysisCenterId());
        plan.setRoomNumber(request.roomNumber());
        plan.setMachineId(request.machineId());
        plan.setStatus(requestedStatus);
        return repository.save(plan);
    }

    public DialysisPlan getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Dialysis plan not found: " + id));
    }

    public List<DialysisPlan> getAll() {
        return repository.findAll();
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private void validateStatusTransition(String currentStatus, String requestedStatus) {
        Set<String> allowedTargets = ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of(currentStatus));
        if (!allowedTargets.contains(requestedStatus)) {
            throw new BusinessException(
                "Invalid transition from " + currentStatus + " to " + requestedStatus + "."
            );
        }
    }
}
