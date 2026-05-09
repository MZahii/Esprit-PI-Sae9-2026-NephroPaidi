package tn.esprit.spring.communicationservice.integration.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StaffSearchResponse {
    private List<UserSummary> items;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
