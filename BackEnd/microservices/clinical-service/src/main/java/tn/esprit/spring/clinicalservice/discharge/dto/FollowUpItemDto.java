package tn.esprit.spring.clinicalservice.discharge.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpItemDto {
    private UUID id;
    private String itemType;
    private String description;
    private String frequency;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
}
