package tn.esprit.spring.procedureservice.surgical.service;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.shared.exception.BusinessException;
import tn.esprit.spring.procedureservice.shared.exception.NotFoundException;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgeryRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.CreateSurgeryRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateSurgeryRequestStatusRequest;
import tn.esprit.spring.procedureservice.surgical.repository.SurgeryRequestRepository;

@Service
public class SurgeryRequestService {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
        "PENDING",
        "PLANNED",
        "REJECTED",
        "CANCELLED"
    );

    private final SurgeryRequestRepository repository;

    public SurgeryRequestService(SurgeryRequestRepository repository) {
        this.repository = repository;
    }

    public SurgeryRequest create(CreateSurgeryRequest request) {
        SurgeryRequest surgeryRequest = SurgeryRequest.builder()
            .patientId(request.patientId().trim())
            .consultationId(request.consultationId().trim())
            .requestedByDoctorId(request.requestedByDoctorId().trim())
            .patientFirstName(request.patientFirstName().trim())
            .patientLastName(request.patientLastName().trim())
            .reason(request.reason().trim())
            .urgencyLevel(normalizeStatus(request.urgencyLevel()))
            .clinicalNote(trimToNull(request.clinicalNote()))
            .status("PENDING")
            .build();

        return repository.save(surgeryRequest);
    }

    public List<SurgeryRequest> getAll(String consultationId, String status) {
        String normalizedConsultationId = trimToNull(consultationId);
        String normalizedStatus = normalizeNullableStatus(status);

        if (normalizedConsultationId != null) {
            return repository.findByConsultationIdOrderByCreatedAtDesc(normalizedConsultationId);
        }

        if (normalizedStatus != null) {
            validateStatus(normalizedStatus);
            return repository.findByStatusOrderByCreatedAtDesc(normalizedStatus);
        }

        return repository.findAllByOrderByCreatedAtDesc();
    }

    public SurgeryRequest updateStatus(Long id, UpdateSurgeryRequestStatusRequest request) {
        SurgeryRequest surgeryRequest = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Surgery request not found: " + id));

        String status = normalizeStatus(request.status());
        validateStatus(status);
        surgeryRequest.setStatus(status);
        return repository.save(surgeryRequest);
    }

    private void validateStatus(String status) {
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BusinessException("Invalid surgery request status: " + status);
        }
    }

    private String normalizeNullableStatus(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : normalizeStatus(trimmed);
    }

    private String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
