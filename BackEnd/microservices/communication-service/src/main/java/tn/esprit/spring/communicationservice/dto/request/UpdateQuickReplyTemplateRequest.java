package tn.esprit.spring.communicationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;

@Getter
@Setter
public class UpdateQuickReplyTemplateRequest {

    @NotBlank
    @Size(max = 60)
    private String name;

    @NotNull
    private MessageType messageType;

    @NotBlank
    @Size(max = 2000)
    private String templateText;
}
