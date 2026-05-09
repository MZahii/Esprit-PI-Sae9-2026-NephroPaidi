package tn.esprit.spring.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dialysis_stock")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DialysisStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private DialysisItem item;

    @Column(nullable = false)
    private Integer quantityAvailable;

    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    void touch() { this.updatedAt = LocalDateTime.now(); }

    public void deduct(int qty) {
        if (qty > this.quantityAvailable) throw new IllegalStateException("Insufficient dialysis material stock");
        this.quantityAvailable -= qty;
    }

    public void add(int qty) {
        this.quantityAvailable += qty;
    }
}