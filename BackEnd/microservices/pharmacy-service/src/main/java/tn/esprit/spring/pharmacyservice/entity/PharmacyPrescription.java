package tn.esprit.spring.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pharmacy_prescriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PharmacyPrescription {

    public enum PrescriptionStatus { PENDING, PROCESSING, DISPENSED, CANCELLED }
    public enum Urgency { STAT, URGENT, ROUTINE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cross-service reference to the consultation that generated this prescription */
    private String consultationId;

    private Long patientId;
    private String patientName;

    private String doctorId;
    private String doctorName;

    /** STAT = immediate life-threatening | URGENT = within 2h | ROUTINE = standard queue */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Urgency urgency = Urgency.ROUTINE;

    /** JSON array of medication lines: [{medicationId, name, quantity, dosage, route, frequency, instructions}] */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String medicationsJson;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PrescriptionStatus status = PrescriptionStatus.PENDING;

    private LocalDateTime receivedAt;

    /** Set when pharmacist has reviewed the prescription (allergy check, dose check) */
    private String verifiedBy;
    private LocalDateTime verifiedAt;

    /** Pharmacist confirms patient has no contraindicated allergies */
    private Boolean allergyConfirmed;

    private LocalDateTime processedAt;
    private String processedBy;

    @PrePersist
    void prePersist() {
        if (this.receivedAt == null) this.receivedAt = LocalDateTime.now();
    }
}
