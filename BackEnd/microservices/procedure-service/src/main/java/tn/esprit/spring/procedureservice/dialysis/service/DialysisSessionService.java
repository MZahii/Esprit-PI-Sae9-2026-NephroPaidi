package tn.esprit.spring.procedureservice.dialysis.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisPlan;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisSession;
import tn.esprit.spring.procedureservice.dialysis.dto.request.CreateDialysisSessionRequest;
import tn.esprit.spring.procedureservice.dialysis.repository.DialysisSessionRepository;
import tn.esprit.spring.procedureservice.shared.exception.BusinessException;
import tn.esprit.spring.procedureservice.shared.exception.NotFoundException;

@Service
public class DialysisSessionService {
    private final DialysisSessionRepository repository;
    private final DialysisPlanService planService;

    public DialysisSessionService(DialysisSessionRepository repository, DialysisPlanService planService) {
        this.repository = repository;
        this.planService = planService;
    }

    public DialysisSession create(CreateDialysisSessionRequest request) {
        // Idempotency check: if idempotency_key provided, return existing session if present
        if (request.idempotencyKey() != null && !request.idempotencyKey().isEmpty()) {
            var existingSession = repository.findByIdempotencyKey(request.idempotencyKey());
            if (existingSession.isPresent()) {
                return existingSession.get();  // Duplicate request, return existing session
            }
        }
        
        // Create new session
        DialysisSession session = new DialysisSession();
        session.setPlan(planService.getById(request.planId()));
        session.setPatientId(request.patientId());
        session.setConsultationId(request.consultationId());
        session.setAppointmentId(request.appointmentId());
        session.setSessionDate(request.sessionDate());
        session.setNotes(request.notes());
        session.setIdempotencyKey(request.idempotencyKey());  // Store idempotency key
        return repository.save(session);
    }

    public DialysisSession getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Dialysis session not found: " + id));
    }

    public List<DialysisSession> getAll() {
        return repository.findAll();
    }

    public List<DialysisSession> generateFromPlan(Long planId) {
        DialysisPlan plan = planService.getById(planId);
        String status = plan.getStatus() == null ? "" : plan.getStatus().trim().toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(status) || "ARCHIVED".equals(status)) {
            throw new BusinessException("Cannot generate sessions for a " + status + " dialysis plan.");
        }

        LocalDate startDate = plan.getStartDate() != null ? plan.getStartDate() : LocalDate.now();
        LocalDate endDate = plan.getEndDate() != null ? plan.getEndDate() : startDate.plusWeeks(4).minusDays(1);
        Set<DayOfWeek> plannedDays = parseDaysOfWeek(plan.getDaysOfWeek());

        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
            if (!plannedDays.contains(current.getDayOfWeek())) {
                continue;
            }

            LocalDateTime sessionDateTime = LocalDateTime.of(current, LocalTime.of(8, 0));

            if (repository.existsByPlanIdAndSessionDate(planId, sessionDateTime)) {
                continue;
            }

            DialysisSession session = new DialysisSession();
            session.setPlan(plan);
            session.setSessionDate(sessionDateTime);
            session.setNotes(buildGeneratedNotes(plan));
            repository.save(session);
        }

        return repository.findByPlanIdOrderBySessionDateAsc(planId);
    }

    private Set<DayOfWeek> parseDaysOfWeek(String rawDays) {
        if (rawDays == null || rawDays.isBlank()) {
            return Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        }

        Set<DayOfWeek> parsed = new HashSet<>();
        Arrays.stream(rawDays.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .forEach(day -> parsed.add(toDayOfWeek(day)));

        return parsed.isEmpty() ? Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY) : parsed;
    }

    private DayOfWeek toDayOfWeek(String dayLabel) {
        String normalized = dayLabel.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MONDAY", "MON" -> DayOfWeek.MONDAY;
            case "TUESDAY", "TUE", "TUES" -> DayOfWeek.TUESDAY;
            case "WEDNESDAY", "WED" -> DayOfWeek.WEDNESDAY;
            case "THURSDAY", "THU", "THURS" -> DayOfWeek.THURSDAY;
            case "FRIDAY", "FRI" -> DayOfWeek.FRIDAY;
            case "SATURDAY", "SAT" -> DayOfWeek.SATURDAY;
            case "SUNDAY", "SUN" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Invalid day of week: " + dayLabel);
        };
    }

    private String buildGeneratedNotes(DialysisPlan plan) {
        return "Generated from plan: type=" + safe(plan.getDialysisType())
            + ", duration=" + safe(plan.getSessionDurationMinutes()) + "min"
            + ", sessionsPerWeek=" + safe(plan.getSessionsPerWeek());
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
