package tn.esprit.spring.clinicalservice.enums;

/**
 * Grading system for Vesicoureteral Reflux (VUR) based on voiding cystourethrogram (VCUG)
 */
public enum VURGrade {
    NONE("No reflux"),
    GRADE_1("Reflux into ureter only, no dilation"),
    GRADE_2("Reflux into ureter and renal pelvis, no dilation"),
    GRADE_3("Mild dilation of ureter, renal pelvis, and calyces"),
    GRADE_4("Moderate dilation with blunting of calyceal fornices"),
    GRADE_5("Severe dilation with loss of normal pelvic anatomy and clubbed calyces");

    private final String description;

    VURGrade(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
