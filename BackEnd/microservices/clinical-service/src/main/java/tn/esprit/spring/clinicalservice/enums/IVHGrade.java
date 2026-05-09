package tn.esprit.spring.clinicalservice.enums;

/**
 * Intraventricular Hemorrhage (IVH) grading in premature infants
 */
public enum IVHGrade {
    NONE("No IVH"),
    GRADE_1("Grade 1: Hemorrhage limited to germinal matrix"),
    GRADE_2("Grade 2: IVH with blood in ventricles, no dilation"),
    GRADE_3("Grade 3: IVH with ventricular dilation"),
    GRADE_4("Grade 4: Parenchymal hemorrhagic infarction");

    private final String description;

    IVHGrade(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
