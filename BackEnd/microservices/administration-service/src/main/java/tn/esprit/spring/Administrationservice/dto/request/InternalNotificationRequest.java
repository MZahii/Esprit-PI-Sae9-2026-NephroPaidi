package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalNotificationRequest {

    @NotBlank
    private String type;

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    private Long targetUserId;
}

