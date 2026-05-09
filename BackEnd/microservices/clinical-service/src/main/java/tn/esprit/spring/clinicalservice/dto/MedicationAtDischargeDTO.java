package tn.esprit.spring.clinicalservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for MedicationAtDischarge
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationAtDischargeDTO {
    private UUID id;
    private UUID dischargeId;
    private String medicationName;
    private BigDecimal dosageValue;
    private String dosageUnit;
    private String routeOfAdministration;
    private String frequency;
    private String medicationStatus;
    private String indication;
    private String modificationJustification;
}
