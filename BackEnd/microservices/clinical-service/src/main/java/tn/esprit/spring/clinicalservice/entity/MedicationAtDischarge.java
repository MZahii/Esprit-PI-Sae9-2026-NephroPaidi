package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.spring.clinicalservice.enums.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Medication at discharge entity.
 * Lists medications prescribed at hospital discharge.
 */
@Entity
@Table(name = "medications_at_discharge", indexes = {
    @Index(name = "idx_discharge_med_id", columnList = "discharge_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationAtDischarge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "discharge_id", nullable = false)
    private UUID dischargeId;
    
    @Column(nullable = false)
    private String medicationName;
    
    @Column(name = "dosage_value", precision = 8, scale = 2)
    private BigDecimal dosageValue;
    
    @Column(name = "dosage_unit")
    private String dosageUnit;  // e.g., "mg", "mL"
    
    @Enumerated(EnumType.STRING)
    @Column(name = "route_of_admin")
    private RouteOfAdministration routeOfAdministration;
    
    @Column(name = "frequency")
    private String frequency;  // e.g., "twice daily", "every 8 hours"
    
    @Enumerated(EnumType.STRING)
    @Column(name = "medication_status")
    private MedicationStatus medicationStatus;
    
    @Column(columnDefinition = "TEXT")
    private String indication;
    
    @Column(columnDefinition = "TEXT")
    private String modificationJustification;  // required if status = MODIFIED or STOPPED
}
