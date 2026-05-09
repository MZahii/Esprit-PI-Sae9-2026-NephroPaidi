package tn.esprit.spring.opsservice.dossier.service;

import tn.esprit.spring.opsservice.dossier.model.ActorRole;

import java.util.UUID;

public record ActorContext(UUID actorId, ActorRole actorRole, String actorDisplayName) {
}
