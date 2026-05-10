package tn.esprit.spring.procedureservice.surgical.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "surgical_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurgicalCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // External references are stored as text in the current schema.
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "consultation_id")
    private String consultationId;

    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "surgery_request_id")
    private Long surgeryRequestId;

    // Patient information (denormalized for read performance)
    private String firstName;
    private String lastName;
    private Integer age;
    private String gender;
    private String medicalRecordNumber;

    // Surgery details
    private String surgeryType;
    private String procedureName;
    private String surgeryCategory;
    private String urgencyLevel;

    // External staff references are stored as text in the current schema.
    private String surgeonId;

    private String assistantSurgeonId;

    private String anesthesiologistId;

    private String nurseTeam;

    // Scheduling
    private LocalDate scheduledDate;
    private LocalTime scheduledStartTime;
    private Integer estimatedDurationMinutes;
    private String operatingRoom;

    // Status
    private String status;
    private String offerStatus;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
