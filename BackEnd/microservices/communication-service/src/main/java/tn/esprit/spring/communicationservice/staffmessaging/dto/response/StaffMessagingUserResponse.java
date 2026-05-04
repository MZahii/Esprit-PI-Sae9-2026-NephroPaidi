package tn.esprit.spring.communicationservice.staffmessaging.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;

@Getter
@Builder
public class StaffMessagingUserResponse {
    private String userId;
    private String username;
    private String displayName;
    private InternalStaffRole role;
    private String email;
    private String avatarUrl;
}
