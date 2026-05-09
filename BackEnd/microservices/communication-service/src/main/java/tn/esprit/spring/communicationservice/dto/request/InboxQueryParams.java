package tn.esprit.spring.communicationservice.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import tn.esprit.spring.communicationservice.domain.enums.MessageQueue;
import tn.esprit.spring.communicationservice.domain.enums.MessageStatus;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.domain.enums.PriorityLevel;

import java.time.Instant;

@Getter
@Setter
public class InboxQueryParams {
    private MessageQueue queue;
    private MessageStatus status;
    private PriorityLevel priority;
    private MessageType messageType;
    private Long patientId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant dateTo;
}
