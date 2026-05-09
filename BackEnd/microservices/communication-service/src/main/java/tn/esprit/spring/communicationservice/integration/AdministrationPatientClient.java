package tn.esprit.spring.communicationservice.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tn.esprit.spring.communicationservice.client.AdministrationServiceClientFeign;
import tn.esprit.spring.communicationservice.integration.dto.AdministrationPatientProfile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdministrationPatientClient {

    private final AdministrationServiceClientFeign administrationServiceClientFeign;

    public List<AdministrationPatientProfile> getByGuardianUserId(Long guardianUserId) {
        return administrationServiceClientFeign.getPatientsByGuardianId(guardianUserId, currentAuthorizationHeader());
    }

    private String currentAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getRequest() == null) {
            return null;
        }

        String authorization = attributes.getRequest().getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        return authorization.trim();
    }
}
