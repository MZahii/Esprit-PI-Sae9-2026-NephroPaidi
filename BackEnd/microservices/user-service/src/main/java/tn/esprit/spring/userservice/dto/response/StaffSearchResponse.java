package tn.esprit.spring.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StaffSearchResponse {
    private List<UserResponse> items;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
