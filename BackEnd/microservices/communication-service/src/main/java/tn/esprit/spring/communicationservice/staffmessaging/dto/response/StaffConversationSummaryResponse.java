package tn.esprit.spring.communicationservice.staffmessaging.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.staffmessaging.domain.StaffConversationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class StaffConversationSummaryResponse {
    private UUID id;
    private StaffConversationType type;
    private String title;
    private String createdByUserId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastMessageAt;
    private long unreadCount;
    private StaffMessageResponse lastMessage;
    private List<StaffConversationParticipantResponse> participants;
}
