package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.dto.request.BulkMessageAction;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class BulkMessageOperationResponse {
    private BulkMessageAction action;
    private int requestedCount;
    private int successCount;
    private int failedCount;
    private List<UUID> succeededIds;
    private List<BulkMessageOperationFailureResponse> failures;
}
