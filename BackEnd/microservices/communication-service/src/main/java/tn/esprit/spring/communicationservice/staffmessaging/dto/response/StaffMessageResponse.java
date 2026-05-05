package tn.esprit.spring.communicationservice.staffmessaging.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;
import tn.esprit.spring.communicationservice.staffmessaging.domain.StaffMessageType;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class StaffMessageResponse {
    private UUID id;
    private UUID conversationId;
    private String senderId;
    private InternalStaffRole senderRole;
    private String senderDisplayName;
    private String content;
    private StaffMessageType messageType;
    private Instant createdAt;
    private Instant editedAt;
    private Instant deletedAt;
}
