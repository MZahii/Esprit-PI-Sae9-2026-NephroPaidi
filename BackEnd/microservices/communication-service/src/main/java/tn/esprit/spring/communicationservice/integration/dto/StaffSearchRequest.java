package tn.esprit.spring.communicationservice.integration.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StaffSearchRequest {
    private String query;
    private List<String> roles;
    private Boolean enabled;
    private String sortBy = "firstName";
    private String sortDir = "asc";
    private int page = 0;
    private int size = 20;
}
