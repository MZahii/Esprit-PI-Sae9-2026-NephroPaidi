package tn.esprit.spring.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.userservice.entity.AccountStatus;
import tn.esprit.spring.userservice.entity.Role;
import tn.esprit.spring.userservice.entity.Sex;
import tn.esprit.spring.userservice.entity.User;

@Getter
@Builder
public class MyAccountSettingsResponse {
    private Long id;
    private String keycloakId;
    private String username;
    private String cin;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Sex sex;
    private Role role;
    private AccountStatus accountStatus;
    private boolean enabled;
    private String avatarUrl;
    private String preferredLanguage;
    private boolean notificationsEnabled;
    private String theme;
    private boolean mustChangePassword;

    public static MyAccountSettingsResponse from(User user) {
        return MyAccountSettingsResponse.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .cin(user.getCin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .sex(user.getSex())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .enabled(user.isEnabled())
                .avatarUrl(user.getAvatarUrl())
                .preferredLanguage(user.getPreferredLanguage())
                .notificationsEnabled(user.isNotificationsEnabled())
                .theme(user.getTheme())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }
}
