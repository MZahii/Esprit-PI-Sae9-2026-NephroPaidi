package tn.esprit.spring.opsservice.hospitalization.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hospitalization_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalizationTask {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospitalization_id", nullable = false)
    private HospitalizationCase hospitalizationCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private HospitalizationTaskType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 1000)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_kind", nullable = false, length = 20)
    private HospitalizationMeasurementKind measurementKind;

    @Column(name = "expected_unit", length = 32)
    private String expectedUnit;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HospitalizationTaskStatus status;

    @Column(name = "latest_note", length = 2000)
    private String latestNote;

    @Column(name = "latest_numeric_value", precision = 10, scale = 2)
    private BigDecimal latestNumericValue;

    @Column(name = "latest_text_value", length = 255)
    private String latestTextValue;

    @Column(name = "latest_unit", length = 32)
    private String latestUnit;

    @Column(name = "last_updated_by_nurse_id", length = 128)
    private String lastUpdatedByNurseId;

    @Column(name = "last_updated_by_nurse_username", length = 120)
    private String lastUpdatedByNurseUsername;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("recordedAt ASC")
    @Builder.Default
    private List<HospitalizationTaskExecution> executions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = HospitalizationTaskStatus.PENDING;
        }
        if (measurementKind == null) {
            measurementKind = HospitalizationMeasurementKind.NONE;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
}
