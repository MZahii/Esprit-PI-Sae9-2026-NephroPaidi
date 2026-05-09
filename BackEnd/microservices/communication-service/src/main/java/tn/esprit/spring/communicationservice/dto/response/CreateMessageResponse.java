package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.domain.enums.MessageQueue;
import tn.esprit.spring.communicationservice.domain.enums.MessageStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CreateMessageResponse {
    private UUID id;
    private MessageStatus status;
    private MessageQueue queue;
    private Instant createdAt;
}
