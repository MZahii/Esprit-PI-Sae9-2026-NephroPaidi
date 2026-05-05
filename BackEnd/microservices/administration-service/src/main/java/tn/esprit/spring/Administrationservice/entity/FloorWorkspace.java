package tn.esprit.spring.Administrationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "floor_workspaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorWorkspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private HospitalFloor floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "workspace_type", nullable = false, length = 40)
    private WorkspaceType workspaceType;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "workspace_code", nullable = false, length = 80)
    private String workspaceCode;

    @Column(name = "workspace_name", nullable = false, length = 180)
    private String workspaceName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
