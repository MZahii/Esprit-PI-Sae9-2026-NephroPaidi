package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationLabRequestsResponse {
    private UUID consultationId;
    private String labRequests;
    private LocalDateTime updatedAt;
}
