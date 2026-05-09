package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MedicationOrderResponse {
    private UUID id;
    private UUID consultationId;
    private String medicationName;
    private Double doseMg;
    private Integer frequencyPerDay;
    private Integer durationDays;
    private String note;
    private Double weightKg;
    private Integer ageYears;
    private String validationStatus;
    private String validationMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
