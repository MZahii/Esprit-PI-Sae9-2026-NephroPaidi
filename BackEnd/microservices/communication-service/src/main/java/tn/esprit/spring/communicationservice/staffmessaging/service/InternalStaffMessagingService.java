package tn.esprit.spring.communicationservice.staffmessaging.service;

import tn.esprit.spring.communicationservice.staffmessaging.dto.request.CreateDirectStaffConversationRequest;
import tn.esprit.spring.communicationservice.staffmessaging.dto.request.SendStaffMessageRequest;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffConversationSummaryResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessageResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessagingUserResponse;

import java.util.List;
import java.util.UUID;

public interface InternalStaffMessagingService {
    List<StaffMessagingUserResponse> listAvailableStaffUsers(String query, int limit);

    StaffConversationSummaryResponse createOrGetDirectConversation(CreateDirectStaffConversationRequest request);

    List<StaffConversationSummaryResponse> listMyConversations();

    List<StaffMessageResponse> listConversationMessages(UUID conversationId);

    StaffMessageResponse sendMessage(UUID conversationId, SendStaffMessageRequest request);

    StaffConversationSummaryResponse markConversationRead(UUID conversationId);
}
