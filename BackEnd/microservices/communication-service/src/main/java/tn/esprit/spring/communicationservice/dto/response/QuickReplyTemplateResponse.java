package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.domain.enums.StaffRole;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
public class QuickReplyTemplateResponse {
    private UUID id;
    private String name;
    private MessageType messageType;
    private StaffRole staffRole;
    private String templateText;
    private long usageCount;
    private Instant createdAt;
    private Instant updatedAt;
}
