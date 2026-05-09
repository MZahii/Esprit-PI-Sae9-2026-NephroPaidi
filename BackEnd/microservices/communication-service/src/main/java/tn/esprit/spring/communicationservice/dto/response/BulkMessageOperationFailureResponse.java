package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class BulkMessageOperationFailureResponse {
    private UUID messageId;
    private String error;
}
