package tn.esprit.spring.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockId;

    @Column(nullable = false, unique = true)
    private Long batchId; // FK to Batch (cross-reference)

    @Column(nullable = false)
    private Integer quantityAvailable;

    public void updateStock(int qty) {
        this.quantityAvailable += qty;
        if (this.quantityAvailable < 0) {
            throw new IllegalStateException("Stock cannot go below 0");
        }
    }

    public boolean checkAvailability(int requested) {
        return this.quantityAvailable >= requested;
    }
}
