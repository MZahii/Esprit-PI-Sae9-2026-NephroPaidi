package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientMedicalDossierResponse {

    private Long patientId;
    private String patientName;
    private UUID sourceConsultationId;
    private LocalDateTime generatedAt;

    @Builder.Default
    private List<ConsultationItem> consultations = new ArrayList<>();

    @Builder.Default
    private List<TimelineItem> timeline = new ArrayList<>();

    @Builder.Default
    private Summary summary = Summary.builder().build();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConsultationItem {
        private UUID consultationId;
        private LocalDateTime consultationDate;
        private ConsultationStatus status;
        private LocalDateTime outcomeUpdatedAt;
        private String notes;
        private String diagnosis;
        private String treatmentPlan;
        @Builder.Default
        private List<DocumentItem> labRequests = new ArrayList<>();
        @Builder.Default
        private List<DocumentItem> prescriptions = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimelineItem {
        private String kind;
        private LocalDateTime occurredAt;
        private UUID consultationId;
        private String title;
        private String summary;
        private String details;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentItem {
        private String label;
        private String details;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        @Builder.Default
        private long consultationCount = 0;
        @Builder.Default
        private long labRequestCount = 0;
        @Builder.Default
        private long prescriptionCount = 0;
        @Builder.Default
        private long dossierEntryCount = 0;
    }
}