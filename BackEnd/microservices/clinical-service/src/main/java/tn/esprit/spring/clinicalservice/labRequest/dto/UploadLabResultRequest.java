package tn.esprit.spring.clinicalservice.labRequest.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadLabResultRequest {
    private String filePath;
    private String fileName;
}
