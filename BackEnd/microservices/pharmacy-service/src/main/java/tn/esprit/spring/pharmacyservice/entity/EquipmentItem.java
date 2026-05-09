package tn.esprit.spring.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipment_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private String name;

    private String category; // e.g. SYRINGE, PPE, CONSUMABLE

    private String unit; // e.g. piece, box, pack

    private Integer minimumStock;

    @Column(columnDefinition = "TEXT")
    private String description;
}