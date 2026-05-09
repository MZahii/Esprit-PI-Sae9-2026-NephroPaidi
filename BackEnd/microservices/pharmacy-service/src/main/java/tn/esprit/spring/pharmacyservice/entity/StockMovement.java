package tn.esprit.spring.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovement {

    public enum StockType { EQUIPMENT, DIALYSIS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockType stockType;

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private Integer quantityTaken;

    /** Username or display name of the staff who requested items */
    private String requestedBy;

    /** Role of the requester (NURSE, SURGEON, etc.) */
    private String requestedByRole;

    /** Department or purpose (e.g. "Dialysis session #42", "Emergency ward") */
    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(nullable = false)
    private LocalDateTime takenAt;

    @PrePersist
    void prePersist() {
        if (this.takenAt == null) this.takenAt = LocalDateTime.now();
    }
}