package tn.esprit.spring.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.userservice.entity.AccountStatus;
import tn.esprit.spring.userservice.entity.Role;
import tn.esprit.spring.userservice.entity.Sex;
import tn.esprit.spring.userservice.entity.User;

import java.time.LocalDate;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String keycloakId;
    private String username;
    private String cin;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private Sex sex;
    private Role role;
    private AccountStatus accountStatus;
    private boolean enabled;
    private String avatarUrl;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .cin(user.getCin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .sex(user.getSex())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .enabled(user.isEnabled())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}