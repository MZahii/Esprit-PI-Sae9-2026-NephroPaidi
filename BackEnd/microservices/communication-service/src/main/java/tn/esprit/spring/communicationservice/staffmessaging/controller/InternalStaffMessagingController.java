package tn.esprit.spring.communicationservice.staffmessaging.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.communicationservice.staffmessaging.dto.request.CreateDirectStaffConversationRequest;
import tn.esprit.spring.communicationservice.staffmessaging.dto.request.SendStaffMessageRequest;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffConversationSummaryResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessageResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessagingUserResponse;
import tn.esprit.spring.communicationservice.staffmessaging.service.InternalStaffMessagingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/communication/staff")
@RequiredArgsConstructor
public class InternalStaffMessagingController {

    private final InternalStaffMessagingService service;

    @GetMapping("/users")
    public List<StaffMessagingUserResponse> listStaffUsers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return service.listAvailableStaffUsers(q, limit);
    }

    @PostMapping("/conversations/direct")
    public StaffConversationSummaryResponse createOrGetDirectConversation(
            @Valid @RequestBody CreateDirectStaffConversationRequest request
    ) {
        return service.createOrGetDirectConversation(request);
    }

    @GetMapping("/conversations")
    public List<StaffConversationSummaryResponse> listConversations() {
        return service.listMyConversations();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<StaffMessageResponse> listMessages(@PathVariable UUID conversationId) {
        return service.listConversationMessages(conversationId);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public StaffMessageResponse sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendStaffMessageRequest request
    ) {
        return service.sendMessage(conversationId, request);
    }

    @PutMapping("/conversations/{conversationId}/read")
    public StaffConversationSummaryResponse markConversationRead(@PathVariable UUID conversationId) {
        return service.markConversationRead(conversationId);
    }
}
