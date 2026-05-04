package tn.esprit.spring.communicationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.communicationservice.domain.entity.QuickReplyTemplate;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.domain.enums.StaffRole;
import tn.esprit.spring.communicationservice.dto.request.CreateQuickReplyTemplateRequest;
import tn.esprit.spring.communicationservice.dto.request.UpdateQuickReplyTemplateRequest;
import tn.esprit.spring.communicationservice.dto.response.QuickReplyTemplateResponse;
import tn.esprit.spring.communicationservice.repository.QuickReplyTemplateRepository;
import tn.esprit.spring.communicationservice.security.CurrentUserService;
import tn.esprit.spring.communicationservice.service.QuickReplyTemplateService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QuickReplyTemplateServiceImpl implements QuickReplyTemplateService {

    private final QuickReplyTemplateRepository repository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<QuickReplyTemplateResponse> list(MessageType messageType) {
        StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
        List<QuickReplyTemplate> templates = messageType == null
                ? repository.findByStaffRoleOrderByNameAsc(staffRole)
                : repository.findByStaffRoleAndMessageTypeOrderByNameAsc(staffRole, messageType);
        return templates.stream().map(this::toResponse).toList();
    }

    @Override
    public QuickReplyTemplateResponse create(CreateQuickReplyTemplateRequest request) {
        StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
        Instant now = Instant.now();
        QuickReplyTemplate template = new QuickReplyTemplate();
        template.setName(request.getName().trim());
        template.setMessageType(request.getMessageType());
        template.setStaffRole(staffRole);
        template.setTemplateText(request.getTemplateText().trim());
        template.setUsageCount(0);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        return toResponse(repository.save(template));
    }

    @Override
    public QuickReplyTemplateResponse update(UUID id, UpdateQuickReplyTemplateRequest request) {
        StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
        QuickReplyTemplate template = loadTemplateForCurrentRole(id, staffRole);
        template.setName(request.getName().trim());
        template.setMessageType(request.getMessageType());
        template.setStaffRole(staffRole);
        template.setTemplateText(request.getTemplateText().trim());
        template.setUpdatedAt(Instant.now());
        return toResponse(repository.save(template));
    }

    @Override
    public void delete(UUID id) {
        StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
        QuickReplyTemplate template = loadTemplateForCurrentRole(id, staffRole);
        repository.delete(template);
    }

    @Override
    public QuickReplyTemplateResponse incrementUsage(UUID id) {
        StaffRole staffRole = currentUserService.getStaffRoleOrThrow();
        QuickReplyTemplate template = loadTemplateForCurrentRole(id, staffRole);
        template.setUsageCount(template.getUsageCount() + 1);
        template.setUpdatedAt(Instant.now());
        return toResponse(repository.save(template));
    }

    private QuickReplyTemplate loadTemplateForCurrentRole(UUID id, StaffRole staffRole) {
        QuickReplyTemplate template = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        if (template.getStaffRole() != staffRole) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
        }
        return template;
    }

    private QuickReplyTemplateResponse toResponse(QuickReplyTemplate template) {
        return QuickReplyTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .messageType(template.getMessageType())
                .staffRole(template.getStaffRole())
                .templateText(template.getTemplateText())
                .usageCount(template.getUsageCount())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
