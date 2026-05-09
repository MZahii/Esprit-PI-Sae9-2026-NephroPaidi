package tn.esprit.spring.communicationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.communicationservice.dto.request.BulkMessageOperationRequest;
import tn.esprit.spring.communicationservice.dto.request.InboxQueryParams;
import tn.esprit.spring.communicationservice.dto.response.BulkMessageOperationResponse;
import tn.esprit.spring.communicationservice.dto.response.FollowUpMessageResponse;
import tn.esprit.spring.communicationservice.dto.response.StaffDirectoryItemResponse;
import tn.esprit.spring.communicationservice.integration.UserDirectoryClient;
import tn.esprit.spring.communicationservice.integration.dto.UserSummary;
import tn.esprit.spring.communicationservice.service.FollowUpMessageService;

import java.util.List;

@RestController
@RequestMapping("/api/communication/backoffice")
@RequiredArgsConstructor
public class CommunicationBackofficeController {

    private final FollowUpMessageService followUpMessageService;
    private final UserDirectoryClient userDirectoryClient;

    @GetMapping("/inbox")
    public List<FollowUpMessageResponse> inbox(@ModelAttribute InboxQueryParams params) {
        return followUpMessageService.staffInbox(params);
    }

    @PostMapping("/messages/bulk")
    public BulkMessageOperationResponse bulkOperate(@Valid @RequestBody BulkMessageOperationRequest request) {
        return followUpMessageService.bulkOperate(request);
    }

    @GetMapping("/doctors")
    public List<StaffDirectoryItemResponse> doctors() {
        return userDirectoryClient.loadDoctors().stream()
                .map(this::toStaffDirectoryItem)
                .toList();
    }

    private StaffDirectoryItemResponse toStaffDirectoryItem(UserSummary user) {
        return StaffDirectoryItemResponse.builder()
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .build();
    }
}
