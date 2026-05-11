package tn.esprit.spring.clinicalservice.consultation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.consultation.dto.PatientMedicalDossierResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationOutcome;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationOutcomeRepository;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.consultation.service.PatientMedicalDossierService;
import tn.esprit.spring.clinicalservice.entity.MedicalDossierEntry;
import tn.esprit.spring.clinicalservice.patient.PatientDirectoryClient;
import tn.esprit.spring.clinicalservice.patient.dto.PatientSummary;
import tn.esprit.spring.clinicalservice.repository.MedicalDossierRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientMedicalDossierServiceImpl implements PatientMedicalDossierService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationOutcomeRepository outcomeRepository;
    private final MedicalDossierRepository medicalDossierRepository;
    private final PatientDirectoryClient patientDirectoryClient;
    private final ObjectMapper objectMapper;

    @Override
    public PatientMedicalDossierResponse getByConsultationId(UUID consultationId, UUID doctorId) {
        Consultation source = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));

        if (doctorId == null || !source.getDoctorId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: not your consultation");
        }

        return buildMedicalDossier(source.getPatientId(), source.getId());
    }

    @Override
    public PatientMedicalDossierResponse getByPatientId(Long patientId) {
        if (patientId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient not specified");
        }

        return buildMedicalDossier(patientId, null);
    }

    private PatientMedicalDossierResponse buildMedicalDossier(Long patientId, UUID sourceConsultationId) {
        List<Consultation> consultations = consultationRepository.findByPatientId(patientId);
        consultations.sort(Comparator.comparing(Consultation::getDateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<UUID> consultationIds = consultations.stream().map(Consultation::getId).toList();
        Map<UUID, ConsultationOutcome> outcomesByConsultation = new LinkedHashMap<>();
        for (ConsultationOutcome outcome : outcomeRepository.findByConsultationIdIn(consultationIds)) {
            outcomesByConsultation.put(outcome.getConsultationId(), outcome);
        }

        List<MedicalDossierEntry> dossierEntries =
                medicalDossierRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        String patientName = resolvePatientName(patientId);

        List<PatientMedicalDossierResponse.ConsultationItem> consultationItems = new ArrayList<>();
        List<PatientMedicalDossierResponse.TimelineItem> timeline = new ArrayList<>();
        long labRequestCount = 0;
        long prescriptionCount = 0;

        for (Consultation consultation : consultations) {
            ConsultationOutcome outcome = outcomesByConsultation.get(consultation.getId());
            List<PatientMedicalDossierResponse.DocumentItem> labRequests = parseDocumentItems(
                    outcome != null ? outcome.getLabRequests() : null,
                    "Lab request"
            );
            List<PatientMedicalDossierResponse.DocumentItem> prescriptions = parseDocumentItems(
                    outcome != null ? outcome.getPrescriptions() : null,
                    "Prescription"
            );

            labRequestCount += labRequests.size();
            prescriptionCount += prescriptions.size();

            consultationItems.add(PatientMedicalDossierResponse.ConsultationItem.builder()
                    .consultationId(consultation.getId())
                    .consultationDate(consultation.getDateTime())
                    .status(consultation.getStatus())
                    .outcomeUpdatedAt(outcome != null ? outcome.getUpdatedAt() : null)
                    .notes(outcome != null ? outcome.getNotes() : null)
                    .diagnosis(outcome != null ? outcome.getDiagnosis() : null)
                    .treatmentPlan(outcome != null ? outcome.getTreatmentPlan() : null)
                    .labRequests(labRequests)
                    .prescriptions(prescriptions)
                    .build());

            timeline.add(PatientMedicalDossierResponse.TimelineItem.builder()
                    .kind("CONSULTATION")
                    .occurredAt(consultation.getDateTime())
                    .consultationId(consultation.getId())
                    .title("Consultation")
                    .summary(consultation.getStatus() != null ? consultation.getStatus().name() : "CONSULTATION")
                    .details(outcome != null ? compactText(outcome.getNotes(), outcome.getDiagnosis(), outcome.getTreatmentPlan()) : null)
                    .build());

            for (PatientMedicalDossierResponse.DocumentItem item : labRequests) {
                timeline.add(PatientMedicalDossierResponse.TimelineItem.builder()
                        .kind("LAB_REQUEST")
                        .occurredAt(outcome != null && outcome.getUpdatedAt() != null ? outcome.getUpdatedAt() : consultation.getDateTime())
                        .consultationId(consultation.getId())
                        .title(item.getLabel())
                        .summary("Lab request")
                        .details(item.getDetails())
                        .build());
            }

            for (PatientMedicalDossierResponse.DocumentItem item : prescriptions) {
                timeline.add(PatientMedicalDossierResponse.TimelineItem.builder()
                        .kind("PRESCRIPTION")
                        .occurredAt(outcome != null && outcome.getUpdatedAt() != null ? outcome.getUpdatedAt() : consultation.getDateTime())
                        .consultationId(consultation.getId())
                        .title(item.getLabel())
                        .summary("Prescription")
                        .details(item.getDetails())
                        .build());
            }
        }

        for (MedicalDossierEntry entry : dossierEntries) {
            timeline.add(PatientMedicalDossierResponse.TimelineItem.builder()
                    .kind(entry.getEntryType() != null ? entry.getEntryType().name() : "DOSSIER_ENTRY")
                    .occurredAt(entry.getCreatedAt())
                    .title(entry.getEntryType() != null ? entry.getEntryType().name().replace('_', ' ') : "Dossier entry")
                    .summary(entry.getSummary())
                    .details(entry.getSourceServiceId() != null ? "Source service: " + entry.getSourceServiceId() : null)
                    .build());
        }

        timeline.sort(Comparator.comparing(PatientMedicalDossierResponse.TimelineItem::getOccurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        return PatientMedicalDossierResponse.builder()
                .patientId(patientId)
                .patientName(patientName)
                .sourceConsultationId(sourceConsultationId)
                .generatedAt(LocalDateTime.now())
                .consultations(consultationItems)
                .timeline(timeline)
                .summary(PatientMedicalDossierResponse.Summary.builder()
                        .consultationCount(consultationItems.size())
                        .labRequestCount(labRequestCount)
                        .prescriptionCount(prescriptionCount)
                        .dossierEntryCount(dossierEntries.size())
                        .build())
                .build();
    }

    private List<PatientMedicalDossierResponse.DocumentItem> parseDocumentItems(String raw, String fallbackLabel) {
        if (raw == null || raw.trim().isEmpty()) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.isArray()) {
                List<PatientMedicalDossierResponse.DocumentItem> items = new ArrayList<>();
                for (JsonNode child : node) {
                    items.add(parseDocumentItem(child, fallbackLabel));
                }
                return items;
            }
            if (node.isObject()) {
                return List.of(parseDocumentItem(node, fallbackLabel));
            }
        } catch (Exception ignored) {
            // Fall through to raw text handling.
        }

        return List.of(PatientMedicalDossierResponse.DocumentItem.builder()
                .label(fallbackLabel)
                .details(raw.trim())
                .build());
    }

    private PatientMedicalDossierResponse.DocumentItem parseDocumentItem(JsonNode node, String fallbackLabel) {
        String label = firstNonBlank(node, "medication", "test", "label", "title", "name", "code");
        if (label == null) {
            label = fallbackLabel;
        }

        StringBuilder details = new StringBuilder();
        appendDetail(details, node, "dosage", "Dosage");
        appendDetail(details, node, "frequency", "Frequency");
        appendDetail(details, node, "durationDays", "Duration");
        appendDetail(details, node, "urgency", "Urgency");
        appendDetail(details, node, "note", "Note");
        appendDetail(details, node, "instructions", "Instructions");

        if (details.length() == 0) {
            details.append(node.toString());
        }

        return PatientMedicalDossierResponse.DocumentItem.builder()
                .label(label)
                .details(details.toString())
                .build();
    }

    private void appendDetail(StringBuilder details, JsonNode node, String fieldName, String label) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || field.asText().trim().isEmpty()) {
            return;
        }
        if (!details.isEmpty()) {
            details.append(" • ");
        }
        details.append(label).append(": ").append(field.asText().trim());
    }

    private String firstNonBlank(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && !field.isNull()) {
                String value = field.asText().trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private String compactText(String... fragments) {
        StringBuilder builder = new StringBuilder();
        for (String fragment : fragments) {
            if (fragment == null || fragment.trim().isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append(fragment.trim());
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private String resolvePatientName(Long patientId) {
        if (patientId == null) {
            return null;
        }

        Map<Long, PatientSummary> patientMap = patientDirectoryClient.getPatientsByIds(List.of(patientId));
        PatientSummary summary = patientMap.get(patientId);
        if (summary == null) {
            return null;
        }

        String first = summary.getFirstName() == null ? "" : summary.getFirstName().trim();
        String last = summary.getLastName() == null ? "" : summary.getLastName().trim();
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? null : fullName;
    }

}
