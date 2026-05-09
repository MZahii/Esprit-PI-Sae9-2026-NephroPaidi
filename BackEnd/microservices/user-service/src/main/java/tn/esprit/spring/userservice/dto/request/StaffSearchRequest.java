package tn.esprit.spring.userservice.dto.request;

import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.userservice.entity.AccountStatus;
import tn.esprit.spring.userservice.entity.Role;

import java.util.List;

@Getter
@Setter
public class StaffSearchRequest {
    private String query;
    private List<Role> roles;
    private List<AccountStatus> statuses;
    private Boolean enabled;
    private String sortBy = "firstName";
    private String sortDir = "asc";
    private int page = 0;
    private int size = 20;
}
