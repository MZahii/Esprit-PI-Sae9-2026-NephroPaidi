package tn.esprit.spring.clinicalservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for TechnicalAct
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicalActDTO {
    private UUID id;
    private UUID dischargeId;
    private String actName;
    private String indication;
    private LocalDateTime actDate;
    private String findings;
}
