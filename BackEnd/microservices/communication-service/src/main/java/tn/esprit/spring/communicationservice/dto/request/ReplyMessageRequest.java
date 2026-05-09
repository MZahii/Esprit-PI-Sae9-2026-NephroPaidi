package tn.esprit.spring.communicationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReplyMessageRequest {

    @NotNull
    @Size(min = 1, max = 2000)
    private String replyText;
}
