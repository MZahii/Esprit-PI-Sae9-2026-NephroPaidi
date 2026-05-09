package tn.esprit.spring.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.pharmacyservice.websocket.PharmacyWebSocketHandler;

@Service
@RequiredArgsConstructor
public class PharmacyEventPublisher {

    private final PharmacyWebSocketHandler handler;

    public void prescriptionEvent(String eventType, Long id) {
        handler.broadcast("{\"type\":\"" + eventType + "\",\"id\":" + id + "}");
    }

    public void movementEvent(Long id) {
        handler.broadcast("{\"type\":\"MOVEMENT_RECORDED\",\"id\":" + id + "}");
    }

    public void alertEvent() {
        handler.broadcast("{\"type\":\"ALERT_UPDATE\"}");
    }
}
