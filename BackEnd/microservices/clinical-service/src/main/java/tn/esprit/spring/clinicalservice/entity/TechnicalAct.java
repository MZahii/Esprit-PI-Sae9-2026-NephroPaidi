package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Technical act entity for discharge document.
 * Lists medical procedures/acts performed during hospitalization.
 */
@Entity
@Table(name = "technical_acts", indexes = {
    @Index(name = "idx_discharge_id", columnList = "discharge_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalAct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "discharge_id", nullable = false)
    private UUID dischargeId;
    
    @Column(nullable = false)
    private String actName;  // e.g., "Lumbar puncture", "Echocardiography"
    
    @Column(columnDefinition = "TEXT")
    private String indication;
    
    @Column(name = "act_date")
    private LocalDateTime actDate;
    
    @Column(columnDefinition = "TEXT")
    private String findings;
}
