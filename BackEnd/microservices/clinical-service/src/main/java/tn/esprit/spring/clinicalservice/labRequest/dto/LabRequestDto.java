package tn.esprit.spring.clinicalservice.labRequest.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabRequestDto {
    private UUID id;
    private UUID doctorId;
    private Long patientId;
    private UUID consultationId;
    private String testType;
    private List<LabRequestTestItemDto> testItems;
    private String urgency;
    private String status;
    private String notes;
    private String latestAiRecommendation;
    private Double latestAiConfidence;
    private Boolean latestAiRequiresDoctorReview;
    private String latestAiSummary;
    private Boolean latestResultAvailable;
    private String latestResultFileName;
    private LocalDateTime latestResultUploadedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
