package tn.esprit.spring.clinicalservice.surgery.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.clinicalservice.surgery.dto.CreateSurgeryIndicationRequest;
import tn.esprit.spring.clinicalservice.surgery.dto.SurgeryIndicationDto;
import tn.esprit.spring.clinicalservice.surgery.entity.SurgeryIndication;
import tn.esprit.spring.clinicalservice.surgery.repository.SurgeryIndicationRepository;
import tn.esprit.spring.clinicalservice.surgery.service.SurgeryIndicationService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SurgeryIndicationServiceImpl implements SurgeryIndicationService {

    private final SurgeryIndicationRepository surgeryIndicationRepository;

    @Override
    public SurgeryIndicationDto createSurgeryIndication(CreateSurgeryIndicationRequest request, UUID doctorId) {
        log.info("Creating surgery indication for patient {} by doctor {}", request.getPatientId(), doctorId);
        
        SurgeryIndication indication = SurgeryIndication.builder()
                .id(UUID.randomUUID())
                .doctorId(doctorId)
                .patientId(request.getPatientId())
                .urgency(SurgeryIndication.SurgeryUrgency.valueOf(request.getUrgency()))
                .notes(request.getNotes())
                .status(SurgeryIndication.SurgeryStatus.PENDING)
                .build();
        
        indication = surgeryIndicationRepository.save(indication);
        
        // TODO: Send notification to receptionist
        
        return mapToDto(indication);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurgeryIndicationDto> getPendingSurgeryIndications() {
        log.info("Fetching pending surgery indications");
        return surgeryIndicationRepository.findByStatusOrderByCreatedAtDesc(SurgeryIndication.SurgeryStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SurgeryIndicationDto getSurgeryIndicationById(UUID id) {
        return surgeryIndicationRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Surgery indication not found: " + id));
    }

    @Override
    public SurgeryIndicationDto updateStatus(UUID id, String status) {
        log.info("Updating surgery indication {} status to {}", id, status);
        
        SurgeryIndication indication = surgeryIndicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Surgery indication not found: " + id));
        
        indication.setStatus(SurgeryIndication.SurgeryStatus.valueOf(status));
        indication = surgeryIndicationRepository.save(indication);
        
        return mapToDto(indication);
    }

    private SurgeryIndicationDto mapToDto(SurgeryIndication indication) {
        return SurgeryIndicationDto.builder()
                .id(indication.getId())
                .doctorId(indication.getDoctorId())
                .patientId(indication.getPatientId())
                .urgency(indication.getUrgency().toString())
                .notes(indication.getNotes())
                .status(indication.getStatus().toString())
                .createdAt(indication.getCreatedAt())
                .build();
    }
}
