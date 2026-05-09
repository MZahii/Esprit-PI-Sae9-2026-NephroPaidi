package tn.esprit.spring.procedureservice.shared.util;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {
    }

    public static String randomId() {
        return UUID.randomUUID().toString();
    }
}
