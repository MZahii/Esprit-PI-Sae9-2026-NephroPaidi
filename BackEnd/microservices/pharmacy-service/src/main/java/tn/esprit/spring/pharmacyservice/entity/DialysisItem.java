package tn.esprit.spring.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dialysis_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DialysisItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private String name;

    private String category; // e.g. BLOOD_BAG, DIALYZER, TUBING, SESSION_SUPPLY

    private String unit; // e.g. piece, bag, set

    private Integer minimumStock;

    @Column(columnDefinition = "TEXT")
    private String description;
}