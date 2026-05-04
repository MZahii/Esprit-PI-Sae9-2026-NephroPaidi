package tn.esprit.spring.communicationservice.staffmessaging.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;

import java.time.Instant;

@Getter
@Builder
public class StaffConversationParticipantResponse {
    private String userId;
    private InternalStaffRole userRole;
    private String displayName;
    private Instant joinedAt;
    private Instant lastReadAt;
    private boolean archived;
    private boolean muted;
}
