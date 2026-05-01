package tn.esprit.spring.clinicalservice.labRequest.service;

import tn.esprit.spring.clinicalservice.labRequest.dto.CreateLabRequestRequest;
import tn.esprit.spring.clinicalservice.labRequest.dto.LabRequestDto;
import tn.esprit.spring.clinicalservice.labRequest.dto.UploadLabResultRequest;
import tn.esprit.spring.clinicalservice.labRequest.entity.LabRequest;

import java.util.List;
import java.util.UUID;

public interface LabRequestService {
    LabRequestDto createLabRequest(CreateLabRequestRequest request, UUID doctorId);
    
    List<LabRequestDto> getLabRequestsByDoctor(UUID doctorId);
    
    List<LabRequestDto> getPendingLabRequests();
    
    LabRequestDto getLabRequestById(UUID id);
    
    LabRequestDto uploadLabResult(UUID labRequestId, UploadLabResultRequest request, UUID uploadedBy);
    
    LabRequestDto updateLabRequestStatus(UUID id, LabRequest.LabStatus status);
}
