package tn.esprit.spring.opsservice.dossier.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class NurseAssignmentService {

    private final List<UUID> defaultNurseIds;
    private final AtomicInteger idx = new AtomicInteger(0);

    public NurseAssignmentService(@Value("${ops.assignment.default-nurse-ids:}") String configuredNurses) {
        this.defaultNurseIds = new ArrayList<>();
        if (configuredNurses != null && !configuredNurses.isBlank()) {
            Arrays.stream(configuredNurses.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(UUID::fromString)
                    .forEach(defaultNurseIds::add);
        }
    }

    public UUID autoAssignNurse(Long patientId) {
        if (defaultNurseIds.isEmpty()) {
            return null;
        }
        int current = Math.abs(idx.getAndIncrement());
        return defaultNurseIds.get(current % defaultNurseIds.size());
    }
}
