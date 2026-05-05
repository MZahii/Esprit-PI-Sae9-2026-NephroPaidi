package tn.esprit.spring.communicationservice.staffmessaging.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDirectStaffConversationRequest {

    @NotBlank(message = "targetUserId is required")
    private String targetUserId;
}
