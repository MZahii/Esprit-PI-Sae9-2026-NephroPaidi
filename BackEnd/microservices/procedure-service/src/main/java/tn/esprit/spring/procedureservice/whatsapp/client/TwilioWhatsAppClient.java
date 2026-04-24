package tn.esprit.spring.procedureservice.whatsapp.client;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tn.esprit.spring.procedureservice.whatsapp.config.TwilioWhatsAppProperties;

@Component
public class TwilioWhatsAppClient {
    private final RestClient restClient;
    private final TwilioWhatsAppProperties properties;

    public TwilioWhatsAppClient(RestClient.Builder restClientBuilder, TwilioWhatsAppProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl()).build();
        this.properties = properties;
    }

    public void sendMessage(String to, String body) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("From", properties.getFrom());
        form.add("To", to);
        form.add("Body", body);

        restClient.post()
            .uri("/2010-04-01/Accounts/{accountSid}/Messages.json", properties.getAccountSid())
            .headers(headers -> headers.setBasicAuth(properties.getAccountSid(), properties.getAuthToken()))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .toBodilessEntity();
    }
}
