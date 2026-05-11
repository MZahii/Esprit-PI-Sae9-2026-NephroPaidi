package tn.esprit.spring.clinicalservice.enums;

/**
 * Types of hereditary nephropathies and genetic kidney diseases
 */
public enum HereditaryNephropathyType {
    ALPORT_SYNDROME("Alport Syndrome (COL4A mutation)"),
    NPHS1("NPHS1 (Nephrin): Congenital Nephrotic Syndrome of the Finnish type"),
    NPHS2("NPHS2 (Podocin): Autosomal Recessive Focal Segmental Glomerulosclerosis"),
    DENYS_DRASH("Denys-Drash Syndrome (WT1 mutation)"),
    FRASIER("Frasier Syndrome (WT1 mutation variant)"),
    WAGR("WAGR Syndrome (Wilms, Aniridia, Genitourinary, Retardation)"),
    RENAL_COLOBOMA_PAX2("Renal-Coloboma Syndrome (PAX2 mutation)"),
    CYSTS_DIABETES_TCF2("Cysts and Diabetes (TCF2/HNF1B mutation)"),
    TUBEROUS_SCLEROSIS("Tuberous Sclerosis Complex (angiomyolipomas, cystic kidneys)"),
    LAURENCE_MOON_BARDET_BIEDL("Laurence-Moon-Bardet-Biedl Syndrome"),
    HYPEROXALURIA_TYPE1("Primary Hyperoxaluria Type 1 (AGXT mutation)"),
    CYSTINOSIS("Cystinosis (lysosomal storage disease)"),
    NEPHRONOPHTHISIS("Nephronophthisis (ciliopathy)"),
    FABRY("Fabry Disease (alpha-galactosidase A deficiency)"),
    GLYCOGENOSIS("Glycogenosis Type II (Pompe)"),
    ARPKD("Autosomal Recessive Polycystic Kidney Disease"),
    ADPKD("Autosomal Dominant Polycystic Kidney Disease");

    private final String description;

    HereditaryNephropathyType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
