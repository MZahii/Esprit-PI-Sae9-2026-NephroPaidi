package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActionRequiredAlertsResponse {
    private long contractsEndingIn7Days;
    private long contractsEndingIn30Days;
    private long pendingUsersTooLong;
    private long profilesMissingRequiredData;
}
