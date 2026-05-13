package tn.esprit.spring.communicationservice.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.triage")
public class AiTriageProperties {
    private boolean enabled = true;
    private String baseUrl = "http://localhost:8086/api/ai/message-urgency-triage";
    private int timeoutMillis = 1500;
}
