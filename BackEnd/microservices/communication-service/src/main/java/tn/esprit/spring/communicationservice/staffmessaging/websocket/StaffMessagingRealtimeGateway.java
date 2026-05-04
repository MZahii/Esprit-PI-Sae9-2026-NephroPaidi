package tn.esprit.spring.communicationservice.staffmessaging.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaffMessagingRealtimeGateway {

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public void registerSession(String userId, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregisterSession(String userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    public void sendToUser(String userId, Object payload) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (IOException ex) {
            log.warn("Failed to serialize staff messaging payload for user {}", userId, ex);
            return;
        }

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                unregisterSession(userId, session);
                continue;
            }

            try {
                session.sendMessage(new TextMessage(jsonPayload));
            } catch (IOException ex) {
                log.warn("Failed to send staff messaging websocket payload to user {}", userId, ex);
                unregisterSession(userId, session);
            }
        }
    }
}
