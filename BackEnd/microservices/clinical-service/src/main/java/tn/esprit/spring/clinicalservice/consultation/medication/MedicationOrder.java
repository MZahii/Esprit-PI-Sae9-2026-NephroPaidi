package tn.esprit.spring.clinicalservice.consultation.medication;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medication_order")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationOrder {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "consultation_id", nullable = false)
    private UUID consultationId;

    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @Column(name = "dose_mg")
    private Double doseMg;

    @Column(name = "frequency_per_day")
    private Integer frequencyPerDay;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "age_years")
    private Integer ageYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status")
    private DoseValidationStatus validationStatus;

    @Column(name = "validation_message", columnDefinition = "text")
    private String validationMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
