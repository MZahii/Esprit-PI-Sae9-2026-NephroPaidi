package tn.esprit.spring.clinicalservice.labRequest.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.spring.clinicalservice.ai.ClinicalAiGateway;
import tn.esprit.spring.clinicalservice.ai.dto.AiLabAnalysisRequest;
import tn.esprit.spring.clinicalservice.ai.dto.AiLabAnalysisResponse;
import tn.esprit.spring.clinicalservice.consultation.metrics.ConsultationMetrics;
import tn.esprit.spring.clinicalservice.consultation.metrics.ConsultationMetricsRepository;
import tn.esprit.spring.clinicalservice.consultation.metrics.ConsultationMetricsService;
import tn.esprit.spring.clinicalservice.labRequest.dto.CreateLabRequestRequest;
import tn.esprit.spring.clinicalservice.labRequest.dto.LabRequestDto;
import tn.esprit.spring.clinicalservice.labRequest.entity.LabRequest;
import tn.esprit.spring.clinicalservice.labRequest.entity.LabResult;
import tn.esprit.spring.clinicalservice.labRequest.repository.LabRequestRepository;
import tn.esprit.spring.clinicalservice.labRequest.repository.LabResultRepository;
import tn.esprit.spring.clinicalservice.labRequest.service.LabRequestService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LabRequestServiceImpl implements LabRequestService {

    private final LabRequestRepository labRequestRepository;
    private final LabResultRepository labResultRepository;
    private final ConsultationMetricsRepository metricsRepository;
    private final ConsultationMetricsService consultationMetricsService;
    private final ClinicalAiGateway clinicalAiGateway;
    private final ObjectMapper objectMapper;

    @Value("${clinical.lab-storage-dir:data/lab-results}")
    private String labStorageDir;

    @Override
    public LabRequestDto createLabRequest(CreateLabRequestRequest request, UUID doctorId) {
        log.info("Creating lab request for patient {} by doctor {}", request.getPatientId(), doctorId);
        
        LabRequest labRequest = LabRequest.builder()
                .id(UUID.randomUUID())
                .doctorId(doctorId)
                .patientId(request.getPatientId())
                .consultationId(request.getConsultationId())
                .testType(request.getTestType())
                .urgency(LabRequest.LabUrgency.valueOf(request.getUrgency() != null ? request.getUrgency() : "ROUTINE"))
                .status(LabRequest.LabStatus.PENDING)
                .notes(request.getNotes())
                .build();
        
        labRequest = labRequestRepository.save(labRequest);
        
        // TODO: Send notification to lab employee
        
        return mapToDto(labRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabRequestDto> getLabRequestsByDoctor(UUID doctorId) {
        log.info("Fetching lab requests for doctor {}", doctorId);
        return labRequestRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabRequestDto> getPendingLabRequests() {
        log.info("Fetching pending lab requests");
        return labRequestRepository.findPending(LabRequest.LabStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LabRequestDto getLabRequestById(UUID id) {
        return labRequestRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Lab request not found: " + id));
    }

    @Override
    public LabRequestDto uploadLabResult(UUID labRequestId, MultipartFile file, UUID uploadedBy) {
        log.info("Uploading lab result for lab request {}", labRequestId);

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Lab result file is required");
        }
        
        LabRequest labRequest = labRequestRepository.findById(labRequestId)
                .orElseThrow(() -> new RuntimeException("Lab request not found: " + labRequestId));

        try {
            byte[] bytes = file.getBytes();
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "lab-result.bin";
            Path targetDir = Paths.get(labStorageDir).resolve(labRequestId.toString());
            Files.createDirectories(targetDir);
            String storedName = System.currentTimeMillis() + "_" + sanitizeFileName(originalName);
            Path targetFile = targetDir.resolve(storedName);
            Files.write(targetFile, bytes);

            LabResult result = LabResult.builder()
                    .id(UUID.randomUUID())
                    .labRequestId(labRequestId)
                    .filePath(targetFile.toString())
                    .fileName(originalName)
                    .contentType(file.getContentType())
                    .fileSizeBytes(file.getSize())
                    .fileData(bytes)
                    .uploadedBy(uploadedBy)
                    .aiStatus("PENDING")
                    .aiRequiresDoctorReview(Boolean.TRUE)
                    .build();

            ConsultationMetrics metrics = labRequest.getConsultationId() != null
                    ? metricsRepository.findByConsultationId(labRequest.getConsultationId()).orElse(null)
                    : null;

            try {
                AiLabAnalysisResponse aiResponse = clinicalAiGateway.analyzeLabResult(
                        AiLabAnalysisRequest.builder()
                                .consultationId(labRequest.getConsultationId())
                                .patientId(labRequest.getPatientId())
                                .patientAgeYears(metrics != null ? metrics.getAgeYears() : null)
                                .patientSex(metrics != null ? metrics.getPatientSex() : null)
                                .heightCm(metrics != null ? metrics.getHeightCm() : null)
                                .weightKg(metrics != null ? metrics.getWeightKg() : null)
                                .documentType("LAB_RESULT")
                                .fileName(originalName)
                                .contentType(file.getContentType())
                                .fileContentBase64(Base64.getEncoder().encodeToString(bytes))
                                .build()
                );

                applyAiResponse(result, labRequest, aiResponse);
            } catch (Exception aiError) {
                log.error("AI analysis failed for lab request {}", labRequestId, aiError);
                result.setAiStatus("FAILED");
                result.setAiSummary("AI analysis unavailable. Manual doctor review required.");
                result.setAiRequiresDoctorReview(Boolean.TRUE);
                result.setAiRawResponse(aiError.getMessage());
                result.setProcessedAt(LocalDateTime.now());
            }

            labResultRepository.save(result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store lab result file", e);
        }
        
        // Update lab request status to COMPLETED
        labRequest.setStatus(LabRequest.LabStatus.COMPLETED);
        labRequest = labRequestRepository.save(labRequest);
        
        // TODO: Send notification to doctor that results are ready
        
        return mapToDto(labRequest);
    }

    @Override
    public LabRequestDto updateLabRequestStatus(UUID id, LabRequest.LabStatus status) {
        log.info("Updating lab request {} status to {}", id, status);
        
        LabRequest labRequest = labRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lab request not found: " + id));
        
        labRequest.setStatus(status);
        labRequest = labRequestRepository.save(labRequest);
        
        return mapToDto(labRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public LabResult getLatestLabResultForConsultation(UUID consultationId) {
        LabRequest labRequest = labRequestRepository.findTopByConsultationIdOrderByCreatedAtDesc(consultationId)
                .orElseThrow(() -> new RuntimeException("No lab request found for consultation: " + consultationId));

        return labResultRepository.findTopByLabRequestIdOrderByUploadedAtDesc(labRequest.getId())
                .orElseThrow(() -> new RuntimeException("No uploaded lab result found for consultation: " + consultationId));
    }

    private LabRequestDto mapToDto(LabRequest labRequest) {
        LabResult latestResult = labResultRepository.findTopByLabRequestIdOrderByUploadedAtDesc(labRequest.getId()).orElse(null);
        return LabRequestDto.builder()
                .id(labRequest.getId())
                .doctorId(labRequest.getDoctorId())
                .patientId(labRequest.getPatientId())
                .consultationId(labRequest.getConsultationId())
                .testType(labRequest.getTestType())
                .urgency(labRequest.getUrgency().toString())
                .status(labRequest.getStatus().toString())
                .notes(labRequest.getNotes())
                .latestAiRecommendation(latestResult != null ? latestResult.getAiRecommendation() : null)
                .latestAiConfidence(latestResult != null ? latestResult.getAiConfidence() : null)
                .latestAiRequiresDoctorReview(latestResult != null ? latestResult.getAiRequiresDoctorReview() : null)
                .latestAiSummary(latestResult != null ? latestResult.getAiSummary() : null)
                .createdAt(labRequest.getCreatedAt())
                .updatedAt(labRequest.getUpdatedAt())
                .build();
    }

    private void applyAiResponse(LabResult result, LabRequest labRequest, AiLabAnalysisResponse aiResponse) {
        if (aiResponse == null) {
            result.setAiStatus("FAILED");
            result.setAiSummary("AI response was empty. Manual doctor review required.");
            result.setAiRequiresDoctorReview(Boolean.TRUE);
            result.setProcessedAt(LocalDateTime.now());
            return;
        }

        result.setAiStatus(aiResponse.getStatus() != null ? aiResponse.getStatus() : "COMPLETED");
        result.setAiRecommendation(aiResponse.getRecommendation());
        result.setAiConfidence(aiResponse.getConfidence());
        result.setAiSummary(aiResponse.getSummary());
        result.setAiRequiresDoctorReview(aiResponse.getRequiresDoctorReview());
        result.setExtractedCreatinineMgDl(aiResponse.getExtractedCreatinineMgDl());
        result.setExtractedCreatinineUmol(aiResponse.getExtractedCreatinineUmolL());
        result.setExtractedUnit(aiResponse.getExtractedUnit());
        result.setParserConfidence(aiResponse.getParserConfidence());
        result.setAiRawResponse(writeRawResponse(aiResponse));
        result.setProcessedAt(LocalDateTime.now());

        if (labRequest.getConsultationId() != null) {
            consultationMetricsService.applyAiLabAnalysis(
                    labRequest.getConsultationId(),
                    labRequest.getPatientId(),
                    result.getFileName(),
                    aiResponse
            );
        }
    }

    private String writeRawResponse(AiLabAnalysisResponse aiResponse) {
        try {
            return objectMapper.writeValueAsString(aiResponse);
        } catch (JsonProcessingException e) {
            return "{\"serializationError\":\"Unable to serialize AI response\"}";
        }
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "lab-result.bin";
        }
        return originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
