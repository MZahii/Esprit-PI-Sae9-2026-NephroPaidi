package tn.esprit.spring.clinicalservice.discharge.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DischargeFollowUpDto {
    private UUID id;
    private Long patientId;
    private UUID doctorId;
    private String status;
    private LocalDateTime createdAt;
    private List<FollowUpItemDto> items;
}
