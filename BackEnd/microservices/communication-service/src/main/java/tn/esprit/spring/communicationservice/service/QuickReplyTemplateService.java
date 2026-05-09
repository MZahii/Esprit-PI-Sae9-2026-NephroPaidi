package tn.esprit.spring.communicationservice.service;

import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.dto.request.CreateQuickReplyTemplateRequest;
import tn.esprit.spring.communicationservice.dto.request.UpdateQuickReplyTemplateRequest;
import tn.esprit.spring.communicationservice.dto.response.QuickReplyTemplateResponse;

import java.util.List;
import java.util.UUID;

public interface QuickReplyTemplateService {
    List<QuickReplyTemplateResponse> list(MessageType messageType);

    QuickReplyTemplateResponse create(CreateQuickReplyTemplateRequest request);

    QuickReplyTemplateResponse update(UUID id, UpdateQuickReplyTemplateRequest request);

    void delete(UUID id);

    QuickReplyTemplateResponse incrementUsage(UUID id);
}
