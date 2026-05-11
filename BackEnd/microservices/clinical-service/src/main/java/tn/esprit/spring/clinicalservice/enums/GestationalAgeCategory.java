package tn.esprit.spring.clinicalservice.enums;

/**
 * Gestational age classification at birth
 */
public enum GestationalAgeCategory {
    EXTREMELY_PREMATURE("<28 weeks"),
    VERY_PREMATURE("28-<32 weeks"),
    PREMATURE("32-<37 weeks"),
    TERM("37-42 weeks"),
    POST_TERM(">42 weeks");

    private final String weekRange;

    GestationalAgeCategory(String weekRange) {
        this.weekRange = weekRange;
    }

    public String getWeekRange() { return weekRange; }
}
