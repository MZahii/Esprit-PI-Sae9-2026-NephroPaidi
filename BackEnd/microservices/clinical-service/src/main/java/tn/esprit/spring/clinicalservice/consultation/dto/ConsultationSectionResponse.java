package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConsultationSectionResponse {
    private UUID id;
    private UUID consultationId;
    private String sectionType;
    private boolean checked;
    private String content;
    private LocalDateTime updatedAt;
}
