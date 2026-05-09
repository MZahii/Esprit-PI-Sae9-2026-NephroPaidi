package tn.esprit.spring.communicationservice.staffmessaging.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.staffmessaging.websocket.StaffMessagingSocketEventType;

import java.util.UUID;

@Getter
@Builder
public class StaffMessagingSocketEvent {
    private StaffMessagingSocketEventType type;
    private UUID conversationId;
    private StaffConversationSummaryResponse conversation;
    private StaffMessageResponse message;
}
