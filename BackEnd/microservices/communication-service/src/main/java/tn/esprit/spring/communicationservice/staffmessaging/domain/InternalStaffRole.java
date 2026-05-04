package tn.esprit.spring.communicationservice.staffmessaging.domain;

import java.util.Arrays;

public enum InternalStaffRole {
    ADMIN,
    HR,
    DOCTOR,
    NURSE,
    RECEPTIONIST,
    PHARMACIST,
    LAB_AGENT,
    SURGEON;

    public static InternalStaffRole fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
}
