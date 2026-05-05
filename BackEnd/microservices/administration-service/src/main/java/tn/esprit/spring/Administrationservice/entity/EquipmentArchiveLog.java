package tn.esprit.spring.Administrationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_archive_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentArchiveLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private HospitalEquipment equipment;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "snapshot", columnDefinition = "TEXT")
    private String snapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
