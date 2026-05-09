package tn.esprit.spring.userservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAccessUpdateRequest {

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}