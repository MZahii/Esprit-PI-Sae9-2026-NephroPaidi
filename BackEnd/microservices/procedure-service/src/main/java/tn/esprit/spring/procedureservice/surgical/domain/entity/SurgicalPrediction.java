package tn.esprit.spring.procedureservice.surgical.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "surgical_predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurgicalPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surgical_case_id", nullable = false)
    private SurgicalCase surgicalCase;

    @Column(nullable = false)
    private String phase;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "prediction_label", nullable = false)
    private String predictionLabel;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(nullable = false)
    private double probability;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "input_snapshot_json", columnDefinition = "TEXT")
    private String inputSnapshotJson;

    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
