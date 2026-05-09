package tn.esprit.spring.clinicalservice.discharge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "follow_up_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpItem {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "follow_up_id", nullable = false, columnDefinition = "uuid")
    private UUID followUpId;

    @Column(name = "item_type", nullable = false, length = 100)
    private String itemType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "frequency", length = 100)
    private String frequency;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ItemStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = ItemStatus.PENDING;
    }

    public enum ItemStatus {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
