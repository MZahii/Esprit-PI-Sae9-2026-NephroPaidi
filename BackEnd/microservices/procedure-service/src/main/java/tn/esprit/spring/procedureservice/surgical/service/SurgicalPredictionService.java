package tn.esprit.spring.procedureservice.surgical.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.client.MlPredictionClient;
import tn.esprit.spring.procedureservice.shared.exception.NotFoundException;
import tn.esprit.spring.procedureservice.surgical.domain.entity.CareTask;
import tn.esprit.spring.procedureservice.surgical.domain.entity.PostOpObservation;
import tn.esprit.spring.procedureservice.surgical.domain.entity.PreOpAssessment;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgicalCase;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgicalPrediction;
import tn.esprit.spring.procedureservice.surgical.dto.ml.MlPredictionRequest;
import tn.esprit.spring.procedureservice.surgical.dto.ml.MlPredictionResponse;
import tn.esprit.spring.procedureservice.surgical.repository.CareTaskRepository;
import tn.esprit.spring.procedureservice.surgical.repository.ComplicationRepository;
import tn.esprit.spring.procedureservice.surgical.repository.PostOpObservationRepository;
import tn.esprit.spring.procedureservice.surgical.repository.PreOpAssessmentRepository;
import tn.esprit.spring.procedureservice.surgical.repository.SurgicalPredictionRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurgicalPredictionService {

    private final SurgicalCaseService surgicalCaseService;
    private final PreOpAssessmentRepository preOpAssessmentRepository;
    private final PostOpObservationRepository postOpObservationRepository;
    private final ComplicationRepository complicationRepository;
    private final CareTaskRepository careTaskRepository;
    private final SurgicalPredictionRepository surgicalPredictionRepository;
    private final MlPredictionClient mlPredictionClient;

    public SurgicalPrediction runPreOpPrediction(Long surgicalCaseId) {
        SurgicalCase surgicalCase = surgicalCaseService.getById(surgicalCaseId);
        Optional<PreOpAssessment> latestPreOp = preOpAssessmentRepository.findTopBySurgicalCaseIdOrderByIdDesc(surgicalCaseId);
        MlPredictionRequest request = buildPreOpRequest(surgicalCase, latestPreOp.orElse(null));
        MlPredictionResponse response = mlPredictionClient.predictPreOp(request);
        return savePrediction(surgicalCase, request, response);
    }

    public SurgicalPrediction runPostOpPrediction(Long surgicalCaseId) {
        SurgicalCase surgicalCase = surgicalCaseService.getById(surgicalCaseId);
        PostOpObservation latestPostOp = postOpObservationRepository.findBySurgicalCaseIdOrderByIdDesc(surgicalCaseId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No post-op observation found for surgical case " + surgicalCaseId));
        MlPredictionRequest request = buildPostOpRequest(surgicalCase, latestPostOp);
        MlPredictionResponse response = mlPredictionClient.predictPostOp(request);
        return savePrediction(surgicalCase, request, response);
    }

    public List<SurgicalPrediction> findBySurgicalCaseId(Long surgicalCaseId) {
        surgicalCaseService.getById(surgicalCaseId);
        return surgicalPredictionRepository.findBySurgicalCaseIdOrderByCreatedAtDesc(surgicalCaseId);
    }

    private SurgicalPrediction savePrediction(SurgicalCase surgicalCase, MlPredictionRequest request, MlPredictionResponse response) {
        SurgicalPrediction prediction = SurgicalPrediction.builder()
                .surgicalCase(surgicalCase)
                .phase(response.phase())
                .modelName(response.modelName())
                .modelVersion(response.modelVersion())
                .predictionLabel(response.predictionLabel())
                .riskLevel(response.riskLevel())
                .probability(response.probability())
                .recommendation(response.recommendation())
                .inputSnapshotJson(toJson(request))
                .outputJson(toJson(response))
                .build();
        return surgicalPredictionRepository.save(prediction);
    }

    private MlPredictionRequest buildPreOpRequest(SurgicalCase surgicalCase, PreOpAssessment latestPreOp) {
        Map<String, String> features = parseStructuredNotes(latestPreOp != null ? latestPreOp.getNotes() : null);
        return new MlPredictionRequest(
                "PRE_OP",
                surgicalCase.getId(),
                surgicalCase.getPatientId(),
                surgicalCase.getConsultationId(),
                surgicalCase.getAge(),
                surgicalCase.getGender(),
                surgicalCase.getUrgencyLevel(),
                surgicalCase.getSurgeryType(),
                surgicalCase.getProcedureName(),
                null,
                null,
                latestPreOp != null ? latestPreOp.getNotes() : null,
                complicationRepository.findBySurgicalCaseIdOrderByIdDesc(surgicalCase.getId()).size(),
                countOpenCareTasks(surgicalCase.getId()),
                0,
                features
        );
    }

    private MlPredictionRequest buildPostOpRequest(SurgicalCase surgicalCase, PostOpObservation latestPostOp) {
        Map<String, String> features = parseStructuredNotes(latestPostOp != null ? latestPostOp.getNotes() : null);
        return new MlPredictionRequest(
                "POST_OP",
                surgicalCase.getId(),
                surgicalCase.getPatientId(),
                surgicalCase.getConsultationId(),
                surgicalCase.getAge(),
                surgicalCase.getGender(),
                surgicalCase.getUrgencyLevel(),
                surgicalCase.getSurgeryType(),
                surgicalCase.getProcedureName(),
                null,
                null,
                latestPostOp != null ? latestPostOp.getNotes() : null,
                complicationRepository.findBySurgicalCaseIdOrderByIdDesc(surgicalCase.getId()).size(),
                countOpenCareTasks(surgicalCase.getId()),
                0,
                features
        );
    }

    private int countOpenCareTasks(Long surgicalCaseId) {
        List<CareTask> tasks = careTaskRepository.findBySurgicalCaseIdOrderByIdAsc(surgicalCaseId);
        return (int) tasks.stream().filter(task -> !task.isDone()).count();
    }

    private Map<String, String> parseStructuredNotes(String notes) {
        Map<String, String> parsed = new LinkedHashMap<>();
        String raw = notes == null ? "" : notes.trim();
        if (!raw.contains("=")) {
            return parsed;
        }

        for (String part : raw.split(";")) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = part.substring(0, separator).trim();
            String value = part.substring(separator + 1).trim();
            if (!key.isEmpty()) {
                parsed.put(key, value);
            }
        }
        return parsed;
    }

    private String toJson(MlPredictionRequest request) {
        return "{"
                + "\"phase\":\"" + safe(request.phase()) + "\","
                + "\"surgicalCaseId\":" + safeNumber(request.surgicalCaseId()) + ","
                + "\"patientId\":\"" + safe(request.patientId()) + "\","
                + "\"consultationId\":\"" + safe(request.consultationId()) + "\","
                + "\"urgencyLevel\":\"" + safe(request.urgencyLevel()) + "\","
                + "\"procedureName\":\"" + safe(request.procedureName()) + "\","
                + "\"noteText\":\"" + safe(request.noteText()) + "\","
                + "\"features\":" + mapToJson(request.features())
                + "}";
    }

    private String toJson(MlPredictionResponse response) {
        return "{"
                + "\"phase\":\"" + safe(response.phase()) + "\","
                + "\"modelName\":\"" + safe(response.modelName()) + "\","
                + "\"modelVersion\":\"" + safe(response.modelVersion()) + "\","
                + "\"predictionLabel\":\"" + safe(response.predictionLabel()) + "\","
                + "\"riskLevel\":\"" + safe(response.riskLevel()) + "\","
                + "\"probability\":" + response.probability() + ","
                + "\"recommendation\":\"" + safe(response.recommendation()) + "\","
                + "\"explanations\":" + explanationsToJson(response.explanations())
                + "}";
    }

    private String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        return map.entrySet().stream()
                .map(entry -> "\"" + safe(entry.getKey()) + "\":\"" + safe(entry.getValue()) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String explanationsToJson(Map<String, Double> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        return map.entrySet().stream()
                .map(entry -> "\"" + safe(entry.getKey()) + "\":" + entry.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private String safeNumber(Number value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
