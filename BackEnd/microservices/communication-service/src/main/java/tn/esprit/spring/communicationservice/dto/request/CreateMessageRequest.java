package tn.esprit.spring.communicationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.domain.enums.PriorityLevel;

@Getter
@Setter
public class CreateMessageRequest {

    private Long patientId;

    @NotNull
    private MessageType messageType;

    @NotNull
    private PriorityLevel priority;

    @Size(max = 120)
    private String subject;

    @NotNull
    @Size(min = 1, max = 2000)
    private String messageText;
}
