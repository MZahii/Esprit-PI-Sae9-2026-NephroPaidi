package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PharmacyPrescriptionDTO {
    private Long          id;
    private String        consultationId;
    private Long          patientId;
    private String        patientName;
    private String        doctorId;
    private String        doctorName;
    private String        urgency;
    private String        medicationsJson;
    private String        notes;
    private String        status;
    private LocalDateTime receivedAt;
    private String        verifiedBy;
    private LocalDateTime verifiedAt;
    private Boolean       allergyConfirmed;
    private LocalDateTime processedAt;
    private String        processedBy;
}
