package tn.esprit.spring.userservice.service;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.spring.userservice.config.KeycloakAdminConfig;
import tn.esprit.spring.userservice.dto.response.KeycloakTokenResponse;
import tn.esprit.spring.userservice.entity.Role;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final Keycloak keycloak;
    private final KeycloakAdminConfig keycloakConfig;
    private final RestTemplateBuilder restTemplateBuilder;
    private static final String VERIFY_EMAIL_ACTION = "VERIFY_EMAIL";

    public record KeycloakUserState(String email, boolean emailVerified, boolean enabled, List<String> requiredActions) {
        public boolean hasEmail() {
            return email != null && !email.isBlank();
        }
    }

    public String createUser(
            String username,
            String email,
            String firstName,
            String lastName,
            String password,
            String role,
            boolean enabled
    ) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required and cannot be generated from email.");
        }

        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(enabled);
        user.setEmailVerified(false);
        user.setRequiredActions(resolveInitialRequiredActions(email));

        Response response = realmResource.users().create(user);

        try {
            String body = safeReadBody(response);

            log.info("Keycloak create user -> status={}, body={}", response.getStatus(), body);

            if (response.getStatus() == 409) {
                throw new IllegalArgumentException("User already exists in Keycloak (username or email already used).");
            }

            if (response.getStatus() != 201) {
                throw new RuntimeException("Keycloak create user failed. Status=" + response.getStatus() + ", body=" + body);
            }

            String userId = CreatedResponseUtil.getCreatedId(response);
            log.info("Keycloak user created successfully. userId={}", userId);

            try {
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(password);
                credential.setTemporary(false);

                realmResource.users().get(userId).resetPassword(credential);
                log.info("Password initialized successfully for userId={}", userId);

                realmResource.users()
                        .get(userId)
                        .roles()
                        .realmLevel()
                        .add(List.of(realmResource.roles().get(role).toRepresentation()));

                log.info("Role {} assigned successfully to userId={}", role, userId);

                return userId;

            } catch (Exception e) {
                log.error("Keycloak post-create step failed for userId={}", userId, e);

                try {
                    realmResource.users().get(userId).remove();
                    log.warn("Rollback OK: Keycloak user {} deleted after failure.", userId);
                } catch (Exception rollbackEx) {
                    log.error("Rollback failed: could not delete Keycloak user {}", userId, rollbackEx);
                }

                throw new RuntimeException("Keycloak user created, but password/role assignment failed: " + e.getMessage(), e);
            }

        } finally {
            response.close();
        }
    }

    public void updateUserEnabled(String keycloakId, boolean enabled) {
        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());

        UserRepresentation userRepresentation = realmResource.users()
                .get(keycloakId)
                .toRepresentation();

        if (userRepresentation == null) {
            throw new IllegalArgumentException("Keycloak user not found: " + keycloakId);
        }

        userRepresentation.setEnabled(enabled);
        realmResource.users().get(keycloakId).update(userRepresentation);

        log.info("Keycloak user {} activation changed to {}", keycloakId, enabled);
    }

    public void deleteUser(String keycloakId) {
        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        try {
            realmResource.users().get(keycloakId).remove();
            log.warn("Keycloak user {} deleted (compensation rollback).", keycloakId);
        } catch (Exception ex) {
            log.error("Failed to delete Keycloak user {} during compensation rollback", keycloakId, ex);
        }
    }

    public void updateUserProfileAndRole(
            String keycloakId,
            String email,
            String firstName,
            String lastName,
            Role role
    ) {
        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        UserResource userResource = realmResource.users().get(keycloakId);
        UserRepresentation userRepresentation = userResource.toRepresentation();

        if (userRepresentation == null) {
            throw new IllegalArgumentException("Keycloak user not found: " + keycloakId);
        }

        boolean emailChanged = hasEmailChanged(userRepresentation.getEmail(), email);
        userRepresentation.setEmail(email);
        userRepresentation.setFirstName(firstName);
        userRepresentation.setLastName(lastName);
        if (emailChanged) {
            userRepresentation.setEmailVerified(false);
            userRepresentation.setRequiredActions(mergeRequiredActions(userRepresentation.getRequiredActions(), VERIFY_EMAIL_ACTION));
        }
        userResource.update(userRepresentation);

        List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
        List<String> managedRoleNames = Arrays.stream(Role.values())
                .map(Enum::name)
                .toList();

        List<RoleRepresentation> rolesToRemove = currentRoles.stream()
                .filter(r -> managedRoleNames.contains(r.getName()))
                .toList();

        if (!rolesToRemove.isEmpty()) {
            userResource.roles().realmLevel().remove(rolesToRemove);
        }

        userResource.roles().realmLevel()
                .add(List.of(realmResource.roles().get(role.name()).toRepresentation()));

        log.info("Keycloak user {} profile and role updated to {}", keycloakId, role);
        if (emailChanged && keycloakConfig.getVerification().isEnabled()) {
            sendVerificationEmailIfPossible(keycloakId);
        }
    }

    public void updateUserProfile(
            String keycloakId,
            String email,
            String firstName,
            String lastName
    ) {
        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        UserResource userResource = realmResource.users().get(keycloakId);
        UserRepresentation userRepresentation = userResource.toRepresentation();

        if (userRepresentation == null) {
            throw new IllegalArgumentException("Keycloak user not found: " + keycloakId);
        }

        boolean emailChanged = hasEmailChanged(userRepresentation.getEmail(), email);
        userRepresentation.setEmail(email);
        userRepresentation.setFirstName(firstName);
        userRepresentation.setLastName(lastName);
        if (emailChanged) {
            userRepresentation.setEmailVerified(false);
            userRepresentation.setRequiredActions(mergeRequiredActions(userRepresentation.getRequiredActions(), VERIFY_EMAIL_ACTION));
        }
        userResource.update(userRepresentation);
        log.info("Keycloak user {} profile updated", keycloakId);
        if (emailChanged && keycloakConfig.getVerification().isEnabled()) {
            sendVerificationEmailIfPossible(keycloakId);
        }
    }

    public void updatePassword(String keycloakId, String newPassword, boolean temporary) {
        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(temporary);
        realmResource.users().get(keycloakId).resetPassword(credential);
        log.info("Password updated successfully for userId={} (temporary={})", keycloakId, temporary);
    }

    public boolean validateCredentials(String username, String password) {
        String tokenUrl = keycloakConfig.getServerUrl()
                + "/realms/" + keycloakConfig.getRealm()
                + "/protocol/openid-connect/token";

        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakConfig.getAuth().getClientId());
        form.add("client_secret", keycloakConfig.getAuth().getClientSecret());
        form.add("username", username);
        form.add("password", password);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    KeycloakTokenResponse.class
            );
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null;
        } catch (HttpStatusCodeException ex) {
            return false;
        }
    }

    public KeycloakUserState getUserState(String keycloakId) {
        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        UserResource userResource = realmResource.users().get(keycloakId);
        UserRepresentation representation = userResource.toRepresentation();

        if (representation == null) {
            throw new IllegalArgumentException("Keycloak user not found: " + keycloakId);
        }

        List<String> requiredActions = representation.getRequiredActions() == null
                ? List.of()
                : List.copyOf(representation.getRequiredActions());

        return new KeycloakUserState(
                representation.getEmail(),
                Boolean.TRUE.equals(representation.isEmailVerified()),
                Boolean.TRUE.equals(representation.isEnabled()),
                requiredActions
        );
    }

    public String findUserIdByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        List<UserRepresentation> users = realmResource.users().searchByUsername(username, true);
        if (users == null || users.isEmpty()) {
            return null;
        }
        return users.get(0).getId();
    }

    public void ensureEmailVerificationRequired(String keycloakId) {
        if (!keycloakConfig.getVerification().isEnabled()) {
            return;
        }

        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        UserResource userResource = realmResource.users().get(keycloakId);
        UserRepresentation userRepresentation = userResource.toRepresentation();

        if (userRepresentation == null) {
            throw new IllegalArgumentException("Keycloak user not found: " + keycloakId);
        }

        if (userRepresentation.getEmail() == null || userRepresentation.getEmail().isBlank()) {
            log.info("Skipping email verification requirement for user {} because email is missing", keycloakId);
            return;
        }

        userRepresentation.setEmailVerified(false);
        userRepresentation.setRequiredActions(mergeRequiredActions(userRepresentation.getRequiredActions(), VERIFY_EMAIL_ACTION));
        userResource.update(userRepresentation);
    }

    public void sendVerificationEmailIfPossible(String keycloakId) {
        if (!keycloakConfig.getVerification().isEnabled()) {
            return;
        }

        try {
            KeycloakUserState state = getUserState(keycloakId);
            if (!state.hasEmail()) {
                log.info("Skipping verification email for user {} because email is missing", keycloakId);
                return;
            }
            if (state.emailVerified()) {
                log.info("Skipping verification email for user {} because email is already verified", keycloakId);
                return;
            }

            keycloak.realm(keycloakConfig.getRealm())
                    .users()
                    .get(keycloakId)
                    .executeActionsEmail(List.of(VERIFY_EMAIL_ACTION));

            log.info("Verification email sent for user {}", keycloakId);
        } catch (Exception ex) {
            log.warn("Failed to send verification email for user {}: {}", keycloakId, ex.getMessage());
        }
    }

    private String safeReadBody(Response response) {
        try {
            if (response.hasEntity()) {
                return response.readEntity(String.class);
            }
        } catch (Exception e) {
            log.warn("Could not read Keycloak response body", e);
        }
        return "";
    }

    private List<String> resolveInitialRequiredActions(String email) {
        if (!keycloakConfig.getVerification().isEnabled()) {
            return List.of();
        }
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return List.of(VERIFY_EMAIL_ACTION);
    }

    private boolean hasEmailChanged(String currentEmail, String requestedEmail) {
        String current = currentEmail == null ? "" : currentEmail.trim();
        String requested = requestedEmail == null ? "" : requestedEmail.trim();
        return !current.equalsIgnoreCase(requested);
    }

    private List<String> mergeRequiredActions(List<String> existingActions, String requiredAction) {
        Set<String> merged = new LinkedHashSet<>();
        if (existingActions != null) {
            merged.addAll(existingActions);
        }
        merged.add(requiredAction);
        return new ArrayList<>(merged);
    }
}
