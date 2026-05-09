package tn.esprit.spring.communicationservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BulkMessageOperationRequest {

    @NotEmpty
    private List<UUID> messageIds;

    @NotNull
    private BulkMessageAction action;
}
