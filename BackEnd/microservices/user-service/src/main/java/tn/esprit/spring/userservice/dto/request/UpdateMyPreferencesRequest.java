package tn.esprit.spring.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMyPreferencesRequest {

    @NotBlank(message = "Preferred language is required")
    @Pattern(regexp = "^(en|fr|ar)$", message = "Preferred language must be one of: en, fr, ar")
    private String preferredLanguage;

    private Boolean notificationsEnabled;

    @NotBlank(message = "Theme is required")
    @Pattern(regexp = "^(light|dark|system)$", message = "Theme must be one of: light, dark, system")
    private String theme;
}
