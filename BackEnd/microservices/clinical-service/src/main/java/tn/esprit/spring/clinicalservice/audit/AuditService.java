package tn.esprit.spring.clinicalservice.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.clinicalservice.security.ActorInfo;
import tn.esprit.spring.clinicalservice.security.ActorResolver;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ActorResolver actorResolver;

    public void record(String entityType, UUID entityId, String action, String details) {
        ActorInfo actor = actorResolver.resolveCurrent();

        AuditEvent event = AuditEvent.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actorId(actor.getId())
                .actorUsername(actor.getUsername())
                .actorRole(actor.getRole())
                .details(details)
                .build();

        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> listAuditEvents(int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 500), Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditEventRepository.findAll(pageable).getContent();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> listAuditEvents() {
        return auditEventRepository.findAll(
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }
}
