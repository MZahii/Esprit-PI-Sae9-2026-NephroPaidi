package tn.esprit.spring.communicationservice.service;

import tn.esprit.spring.communicationservice.dto.request.BulkMessageOperationRequest;
import tn.esprit.spring.communicationservice.dto.request.CreateMessageRequest;
import tn.esprit.spring.communicationservice.dto.request.EscalateRequest;
import tn.esprit.spring.communicationservice.dto.request.InboxQueryParams;
import tn.esprit.spring.communicationservice.dto.request.ReplyMessageRequest;
import tn.esprit.spring.communicationservice.dto.response.BulkMessageOperationResponse;
import tn.esprit.spring.communicationservice.dto.response.CreateMessageResponse;
import tn.esprit.spring.communicationservice.dto.response.FollowUpMessageResponse;
import tn.esprit.spring.communicationservice.dto.response.MessageAuditLogResponse;

import java.util.List;
import java.util.UUID;

public interface FollowUpMessageService {
    CreateMessageResponse create(CreateMessageRequest request);

    List<FollowUpMessageResponse> myMessages();

    FollowUpMessageResponse getById(UUID id);

    FollowUpMessageResponse take(UUID id);

    FollowUpMessageResponse unassign(UUID id);

    FollowUpMessageResponse markRead(UUID id);

    FollowUpMessageResponse reply(UUID id, ReplyMessageRequest request);

    FollowUpMessageResponse escalate(UUID id, EscalateRequest request);

    FollowUpMessageResponse close(UUID id);

    List<FollowUpMessageResponse> staffInbox(InboxQueryParams params);

    List<MessageAuditLogResponse> audit(UUID id);

    BulkMessageOperationResponse bulkOperate(BulkMessageOperationRequest request);
}
