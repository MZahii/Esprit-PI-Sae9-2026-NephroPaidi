package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.domain.enums.MessageQueue;
import tn.esprit.spring.communicationservice.domain.enums.MessageStatus;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.domain.enums.PriorityLevel;
import tn.esprit.spring.communicationservice.domain.enums.StaffRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class FollowUpMessageResponse {
    private UUID id;
    private Long patientId;
    private String guardianKeycloakId;
    private String assignedDoctorKeycloakId;
    private MessageType messageType;
    private PriorityLevel priority;
    private MessageQueue queue;
    private MessageStatus status;
    private String assignedToUserKeycloakId;
    private StaffRole assignedToRole;
    private String subject;
    private String messageText;
    private Instant createdAt;
    private Instant readAt;
    private Instant lastUpdatedAt;
    private Instant closedAt;
    private List<MessageReplyResponse> replies;
}
