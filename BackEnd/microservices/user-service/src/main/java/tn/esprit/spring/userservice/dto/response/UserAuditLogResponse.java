package tn.esprit.spring.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.userservice.entity.UserAuditLog;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserAuditLogResponse {
    private Long id;
    private Long userId;
    private String action;
    private String actor;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;

    public static UserAuditLogResponse from(UserAuditLog log) {
        return UserAuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .action(log.getAction())
                .actor(log.getActor())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
