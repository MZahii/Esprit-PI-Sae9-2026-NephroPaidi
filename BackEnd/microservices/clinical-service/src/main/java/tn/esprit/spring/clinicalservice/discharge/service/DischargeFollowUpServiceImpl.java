package tn.esprit.spring.clinicalservice.discharge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.spring.clinicalservice.discharge.dto.CreateDischargeFollowUpRequest;
import tn.esprit.spring.clinicalservice.discharge.dto.CreateFollowUpItemRequest;
import tn.esprit.spring.clinicalservice.discharge.dto.DischargeFollowUpDto;
import tn.esprit.spring.clinicalservice.discharge.dto.FollowUpItemDto;
import tn.esprit.spring.clinicalservice.discharge.entity.DischargeFollowUp;
import tn.esprit.spring.clinicalservice.discharge.entity.FollowUpItem;
import tn.esprit.spring.clinicalservice.discharge.repository.DischargeFollowUpRepository;
import tn.esprit.spring.clinicalservice.discharge.repository.FollowUpItemRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DischargeFollowUpServiceImpl implements DischargeFollowUpService {

    private final DischargeFollowUpRepository dischargeFollowUpRepository;
    private final FollowUpItemRepository followUpItemRepository;

    @Override
    public DischargeFollowUpDto createFollowUp(CreateDischargeFollowUpRequest request, UUID doctorId) {
        log.info("Creating discharge follow-up for patient: {} by doctor: {}", request.getPatientId(), doctorId);

        DischargeFollowUp followUp = DischargeFollowUp.builder()
                .patientId(request.getPatientId())
                .doctorId(doctorId)
                .status(DischargeFollowUp.FollowUpStatus.ACTIVE)
                .build();

        DischargeFollowUp savedFollowUp = dischargeFollowUpRepository.save(followUp);

        // Create follow-up items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<FollowUpItem> items = request.getItems().stream()
                    .map(itemRequest -> FollowUpItem.builder()
                            .followUpId(savedFollowUp.getId())
                            .itemType(itemRequest.getItemType())
                            .description(itemRequest.getDescription())
                            .frequency(itemRequest.getFrequency())
                            .status(FollowUpItem.ItemStatus.PENDING)
                            .build())
                    .collect(Collectors.toList());

            followUpItemRepository.saveAll(items);
        }

        // TODO: Call NotificationPublisherService to notify guardian

        return convertToDto(savedFollowUp);
    }

    @Override
    public List<DischargeFollowUpDto> getFollowUpsByPatient(Long patientId) {
        log.info("Fetching follow-ups for patient: {}", patientId);

        List<DischargeFollowUp> followUps = dischargeFollowUpRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return followUps.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<DischargeFollowUpDto> getFollowUpsByDoctor(UUID doctorId) {
        log.info("Fetching follow-ups created by doctor: {}", doctorId);

        List<DischargeFollowUp> followUps = dischargeFollowUpRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
        return followUps.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public DischargeFollowUpDto addFollowUpItem(UUID followUpId, CreateFollowUpItemRequest itemRequest) {
        log.info("Adding item to follow-up: {}", followUpId);

        DischargeFollowUp followUp = dischargeFollowUpRepository.findById(followUpId)
                .orElseThrow(() -> new IllegalArgumentException("Follow-up not found: " + followUpId));

        FollowUpItem item = FollowUpItem.builder()
                .followUpId(followUpId)
                .itemType(itemRequest.getItemType())
                .description(itemRequest.getDescription())
                .frequency(itemRequest.getFrequency())
                .status(FollowUpItem.ItemStatus.PENDING)
                .build();

        followUpItemRepository.save(item);
        return convertToDto(followUp);
    }

    @Override
    public DischargeFollowUpDto updateItemStatus(UUID itemId, String status) {
        log.info("Updating follow-up item {} status to {}", itemId, status);

        FollowUpItem item = followUpItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Follow-up item not found: " + itemId));

        try {
            item.setStatus(FollowUpItem.ItemStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status value: {}", status);
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        followUpItemRepository.save(item);

        DischargeFollowUp followUp = dischargeFollowUpRepository.findById(item.getFollowUpId())
                .orElseThrow(() -> new IllegalArgumentException("Follow-up not found"));

        return convertToDto(followUp);
    }

    private DischargeFollowUpDto convertToDto(DischargeFollowUp followUp) {
        List<FollowUpItemDto> itemDtos = followUpItemRepository.findByFollowUpIdOrderByCreatedAtDesc(followUp.getId())
                .stream()
                .map(item -> FollowUpItemDto.builder()
                        .id(item.getId())
                        .itemType(item.getItemType())
                        .description(item.getDescription())
                        .frequency(item.getFrequency())
                        .startDate(item.getStartDate())
                        .endDate(item.getEndDate())
                        .status(item.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        return DischargeFollowUpDto.builder()
                .id(followUp.getId())
                .patientId(followUp.getPatientId())
                .doctorId(followUp.getDoctorId())
                .status(followUp.getStatus().name())
                .createdAt(followUp.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}
