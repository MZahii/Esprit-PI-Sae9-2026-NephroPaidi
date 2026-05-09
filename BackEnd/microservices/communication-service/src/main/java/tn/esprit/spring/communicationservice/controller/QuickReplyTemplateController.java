package tn.esprit.spring.communicationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.dto.request.CreateQuickReplyTemplateRequest;
import tn.esprit.spring.communicationservice.dto.request.UpdateQuickReplyTemplateRequest;
import tn.esprit.spring.communicationservice.dto.response.QuickReplyTemplateResponse;
import tn.esprit.spring.communicationservice.service.QuickReplyTemplateService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/communication/templates")
@RequiredArgsConstructor
public class QuickReplyTemplateController {

    private final QuickReplyTemplateService service;

    @GetMapping
    public List<QuickReplyTemplateResponse> list(@RequestParam(required = false) MessageType messageType) {
        return service.list(messageType);
    }

    @PostMapping
    public QuickReplyTemplateResponse create(@Valid @RequestBody CreateQuickReplyTemplateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public QuickReplyTemplateResponse update(@PathVariable UUID id,
                                             @Valid @RequestBody UpdateQuickReplyTemplateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/use")
    public QuickReplyTemplateResponse incrementUsage(@PathVariable UUID id) {
        return service.incrementUsage(id);
    }
}
