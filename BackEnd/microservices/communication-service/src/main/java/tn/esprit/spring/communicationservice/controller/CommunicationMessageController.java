package tn.esprit.spring.communicationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.communicationservice.dto.request.CreateMessageRequest;
import tn.esprit.spring.communicationservice.dto.request.EscalateRequest;
import tn.esprit.spring.communicationservice.dto.request.ReplyMessageRequest;
import tn.esprit.spring.communicationservice.dto.response.CreateMessageResponse;
import tn.esprit.spring.communicationservice.dto.response.FollowUpMessageResponse;
import tn.esprit.spring.communicationservice.dto.response.MessageAuditLogResponse;
import tn.esprit.spring.communicationservice.service.FollowUpMessageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/communication/messages")
@RequiredArgsConstructor
public class CommunicationMessageController {

    private final FollowUpMessageService followUpMessageService;

    @PostMapping
    public CreateMessageResponse create(@Valid @RequestBody CreateMessageRequest request) {
        return followUpMessageService.create(request);
    }

    @GetMapping("/my")
    public List<FollowUpMessageResponse> myMessages() {
        return followUpMessageService.myMessages();
    }

    @GetMapping("/{id}")
    public FollowUpMessageResponse getById(@PathVariable UUID id) {
        return followUpMessageService.getById(id);
    }

    @PostMapping("/{id}/reply")
    public FollowUpMessageResponse reply(@PathVariable UUID id, @Valid @RequestBody ReplyMessageRequest request) {
        return followUpMessageService.reply(id, request);
    }

    @PostMapping("/{id}/close")
    public FollowUpMessageResponse close(@PathVariable UUID id) {
        return followUpMessageService.close(id);
    }

    @PostMapping("/{id}/take")
    public FollowUpMessageResponse take(@PathVariable UUID id) {
        return followUpMessageService.take(id);
    }

    @PostMapping("/{id}/unassign")
    public FollowUpMessageResponse unassign(@PathVariable UUID id) {
        return followUpMessageService.unassign(id);
    }

    @PostMapping("/{id}/mark-read")
    public FollowUpMessageResponse markRead(@PathVariable UUID id) {
        return followUpMessageService.markRead(id);
    }

    @PostMapping("/{id}/escalate")
    public FollowUpMessageResponse escalate(@PathVariable UUID id, @RequestBody EscalateRequest request) {
        return followUpMessageService.escalate(id, request);
    }

    @GetMapping("/{id}/audit")
    public List<MessageAuditLogResponse> audit(@PathVariable UUID id) {
        return followUpMessageService.audit(id);
    }
}
