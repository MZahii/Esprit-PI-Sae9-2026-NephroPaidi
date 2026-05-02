package tn.esprit.spring.opsservice.hospitalization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hospitalization_task_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalizationTaskExecution {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospitalization_id", nullable = false)
    private HospitalizationCase hospitalizationCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private HospitalizationTask task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HospitalizationTaskStatus status;

    @Column(name = "nurse_keycloak_id", nullable = false, length = 128)
    private String nurseKeycloakId;

    @Column(name = "nurse_username", nullable = false, length = 120)
    private String nurseUsername;

    @Column(length = 2000)
    private String note;

    @Column(name = "numeric_value", precision = 10, scale = 2)
    private BigDecimal numericValue;

    @Column(name = "text_value", length = 255)
    private String textValue;

    @Column(length = 32)
    private String unit;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
