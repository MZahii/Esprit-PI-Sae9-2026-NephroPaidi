package tn.esprit.spring.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispensation_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DispensationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long batchId;

    /** Denormalized batch number for audit trail without joins */
    private String batchNumber;

    /** Denormalized medication name for audit trail without joins */
    private String medicationName;

    @Column(nullable = false)
    private Integer quantity;

    /** Patient who received the medication */
    private Long patientId;
    private String patientName;

    /** Prescription that authorized this dispensation */
    private Long prescriptionId;

    /** Username of the pharmacist who dispensed */
    private String dispensedBy;

    @Column(nullable = false)
    private LocalDateTime dispensedAt;

    @PrePersist
    void prePersist() {
        if (this.dispensedAt == null) this.dispensedAt = LocalDateTime.now();
    }
}
