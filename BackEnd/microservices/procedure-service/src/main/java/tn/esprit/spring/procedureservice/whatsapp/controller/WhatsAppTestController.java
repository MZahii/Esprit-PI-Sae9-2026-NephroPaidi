package tn.esprit.spring.procedureservice.whatsapp.controller;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.procedureservice.shared.exception.BusinessException;
import tn.esprit.spring.procedureservice.whatsapp.service.TwilioWhatsAppMessageService;

@RestController
@RequestMapping("/api/procedures/whatsapp")
public class WhatsAppTestController {
    private final TwilioWhatsAppMessageService messageService;

    public WhatsAppTestController(TwilioWhatsAppMessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestMessage() {
        if (!messageService.isReady()) {
            throw new BusinessException("Twilio WhatsApp is not configured or disabled.");
        }

        messageService.sendToDefaultRecipient(
            "Test NephroPaidi WhatsApp: integration procedure-service OK - " + OffsetDateTime.now()
        );

        return ResponseEntity.ok(Map.of("status", "sent"));
    }
}
