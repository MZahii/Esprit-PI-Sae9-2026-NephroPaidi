package tn.esprit.spring.procedureservice.whatsapp.service;

import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.whatsapp.client.TwilioWhatsAppClient;
import tn.esprit.spring.procedureservice.whatsapp.config.TwilioWhatsAppProperties;

@Service
public class TwilioWhatsAppMessageService {
    private final TwilioWhatsAppClient client;
    private final TwilioWhatsAppProperties properties;

    public TwilioWhatsAppMessageService(TwilioWhatsAppClient client, TwilioWhatsAppProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public boolean isReady() {
        return properties.isEnabled()
            && hasText(properties.getAccountSid())
            && hasText(properties.getAuthToken())
            && hasText(properties.getFrom())
            && hasText(properties.getDefaultRecipient());
    }

    public void sendToDefaultRecipient(String body) {
        client.sendMessage(properties.getDefaultRecipient(), body);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
