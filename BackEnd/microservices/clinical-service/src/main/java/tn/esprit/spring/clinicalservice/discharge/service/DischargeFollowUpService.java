package tn.esprit.spring.clinicalservice.discharge.service;

import tn.esprit.spring.clinicalservice.discharge.dto.CreateDischargeFollowUpRequest;
import tn.esprit.spring.clinicalservice.discharge.dto.CreateFollowUpItemRequest;
import tn.esprit.spring.clinicalservice.discharge.dto.DischargeFollowUpDto;

import java.util.List;
import java.util.UUID;

public interface DischargeFollowUpService {
    DischargeFollowUpDto createFollowUp(CreateDischargeFollowUpRequest request, UUID doctorId);
    List<DischargeFollowUpDto> getFollowUpsByPatient(Long patientId);
    List<DischargeFollowUpDto> getFollowUpsByDoctor(UUID doctorId);
    DischargeFollowUpDto addFollowUpItem(UUID followUpId, CreateFollowUpItemRequest item);
    DischargeFollowUpDto updateItemStatus(UUID itemId, String status);
}
