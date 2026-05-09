package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.*;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationResponse {

    private UUID id;
    private Long patientId;
    private String patientName;
    private UUID doctorId;
    private LocalDateTime dateTime;
    private UUID appointmentId;
    private LocalDateTime startedAt;
    private ConsultationStatus status;
    private LocalDateTime archivedAt;
    private String archivedBy;
}
