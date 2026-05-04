package tn.esprit.spring.communicationservice.staffmessaging.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendStaffMessageRequest {

    @NotBlank(message = "content is required")
    @Size(max = 4000, message = "content must not exceed 4000 characters")
    private String content;
}
