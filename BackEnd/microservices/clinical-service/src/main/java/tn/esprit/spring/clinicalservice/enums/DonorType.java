package tn.esprit.spring.clinicalservice.enums;

/**
 * Type of kidney donor for transplantation
 */
public enum DonorType {
    LIVING_RELATED("Living related donor (parent, sibling)"),
    LIVING_UNRELATED("Living unrelated donor (spouse, friend)"),
    DECEASED("Deceased donor");

    private final String description;

    DonorType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
