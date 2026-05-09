package tn.esprit.spring.clinicalservice.security;

import org.springframework.stereotype.Component;

@Component
public class GuardianIdResolver {

    public Long resolve(Long headerId) {
        return headerId;
    }
}
