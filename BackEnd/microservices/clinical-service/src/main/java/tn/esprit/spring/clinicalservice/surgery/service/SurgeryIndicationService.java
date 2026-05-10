package tn.esprit.spring.clinicalservice.surgery.service;

import tn.esprit.spring.clinicalservice.surgery.dto.CreateSurgeryIndicationRequest;
import tn.esprit.spring.clinicalservice.surgery.dto.SurgeryIndicationDto;

import java.util.List;
import java.util.UUID;

public interface SurgeryIndicationService {
    SurgeryIndicationDto createSurgeryIndication(CreateSurgeryIndicationRequest request, UUID doctorId);
    List<SurgeryIndicationDto> getPendingSurgeryIndications();
    SurgeryIndicationDto getSurgeryIndicationById(UUID id);
    SurgeryIndicationDto updateStatus(UUID id, String status);
}
