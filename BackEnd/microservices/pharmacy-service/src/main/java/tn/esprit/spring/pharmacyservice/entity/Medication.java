package tn.esprit.spring.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "medications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long medicationId;

    /** Brand / trade name (e.g. Lasix) */
    @Column(nullable = false)
    private String name;

    /** INN — International Nonproprietary Name (e.g. Furosemide) */
    private String genericName;

    /** Dosage form: tablet, capsule, injection, syrup, etc. */
    private String form;

    /** Strength / concentration: e.g. "40 mg", "5 mg/mL", "250 mg/5 mL" */
    private String strength;

    /** Dispensing unit: tablet, mL, unit, sachet, etc. */
    private String unit;

    /** Pharmacological class (e.g. Diuretic, Immunosuppressant, Phosphate Binder) */
    private String therapeuticClass;

    /**
     * Standard / usual dose information for reference
     * (column renamed from pediatric_dosage in V8 migration)
     */
    @Column(name = "standard_dosage")
    private String standardDosage;

    /** If TRUE: dose must be adjusted in patients with renal impairment (critical for nephrology) */
    @Builder.Default
    private boolean renalDoseAdjustment = false;

    /** Storage requirement: ROOM_TEMPERATURE | REFRIGERATED_2_8 | FROZEN | LIGHT_PROTECTED */
    @Builder.Default
    private String storageConditions = "ROOM_TEMPERATURE";

    /** If TRUE: classified as controlled / narcotic substance */
    @Builder.Default
    private boolean controlledSubstance = false;

    /** Minimum stock threshold — triggers a reorder alert when total stock falls below this value */
    private Integer minimumStock;

    @OneToMany(mappedBy = "medication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Batch> batches;
}
