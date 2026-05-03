package tn.esprit.spring.clinicalservice.followup.dto;

import lombok.*;
import tn.esprit.spring.clinicalservice.followup.DoctorFollowUpStatus;
import tn.esprit.spring.clinicalservice.followup.FollowUpOffsetUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorFollowUpResponse {
    private UUID id;
    private UUID consultationId;
    private Long patientId;
    private UUID doctorId;
    private LocalDate anchorDate;
    private Integer offsetAmount;
    private FollowUpOffsetUnit offsetUnit;
    private LocalDate computedReturnDate;
    private DoctorFollowUpStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
