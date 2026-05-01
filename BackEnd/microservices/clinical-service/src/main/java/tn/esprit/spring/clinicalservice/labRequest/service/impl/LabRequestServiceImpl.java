package tn.esprit.spring.clinicalservice.labRequest.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.clinicalservice.labRequest.dto.CreateLabRequestRequest;
import tn.esprit.spring.clinicalservice.labRequest.dto.LabRequestDto;
import tn.esprit.spring.clinicalservice.labRequest.dto.UploadLabResultRequest;
import tn.esprit.spring.clinicalservice.labRequest.entity.LabRequest;
import tn.esprit.spring.clinicalservice.labRequest.entity.LabResult;
import tn.esprit.spring.clinicalservice.labRequest.repository.LabRequestRepository;
import tn.esprit.spring.clinicalservice.labRequest.repository.LabResultRepository;
import tn.esprit.spring.clinicalservice.labRequest.service.LabRequestService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LabRequestServiceImpl implements LabRequestService {

    private final LabRequestRepository labRequestRepository;
    private final LabResultRepository labResultRepository;

    @Override
    public LabRequestDto createLabRequest(CreateLabRequestRequest request, UUID doctorId) {
        log.info("Creating lab request for patient {} by doctor {}", request.getPatientId(), doctorId);
        
        LabRequest labRequest = LabRequest.builder()
                .id(UUID.randomUUID())
                .doctorId(doctorId)
                .patientId(request.getPatientId())
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
    public LabRequestDto uploadLabResult(UUID labRequestId, UploadLabResultRequest request, UUID uploadedBy) {
        log.info("Uploading lab result for lab request {}", labRequestId);
        
        LabRequest labRequest = labRequestRepository.findById(labRequestId)
                .orElseThrow(() -> new RuntimeException("Lab request not found: " + labRequestId));
        
        LabResult result = LabResult.builder()
                .id(UUID.randomUUID())
                .labRequestId(labRequestId)
                .filePath(request.getFilePath())
                .fileName(request.getFileName())
                .uploadedBy(uploadedBy)
                .build();
        
        labResultRepository.save(result);
        
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

    private LabRequestDto mapToDto(LabRequest labRequest) {
        return LabRequestDto.builder()
                .id(labRequest.getId())
                .doctorId(labRequest.getDoctorId())
                .patientId(labRequest.getPatientId())
                .testType(labRequest.getTestType())
                .urgency(labRequest.getUrgency().toString())
                .status(labRequest.getStatus().toString())
                .notes(labRequest.getNotes())
                .createdAt(labRequest.getCreatedAt())
                .updatedAt(labRequest.getUpdatedAt())
                .build();
    }
}
