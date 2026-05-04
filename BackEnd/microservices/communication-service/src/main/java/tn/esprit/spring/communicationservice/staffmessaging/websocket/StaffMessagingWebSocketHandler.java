package tn.esprit.spring.communicationservice.staffmessaging.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class StaffMessagingWebSocketHandler extends TextWebSocketHandler {

    private final StaffMessagingRealtimeGateway realtimeGateway;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = userId(session);
        if (userId != null) {
            realtimeGateway.registerSession(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = userId(session);
        if (userId != null) {
            realtimeGateway.unregisterSession(userId, session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = userId(session);
        if (userId != null) {
            realtimeGateway.unregisterSession(userId, session);
        }
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // This websocket is server-push only. REST remains the write path.
    }

    private String userId(WebSocketSession session) {
        Object value = session.getAttributes().get(StaffMessagingHandshakeInterceptor.ATTR_USER_ID);
        return value instanceof String userId ? userId : null;
    }
}
