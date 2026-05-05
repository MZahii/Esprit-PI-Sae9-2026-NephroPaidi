package tn.esprit.spring.communicationservice.staffmessaging.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final StaffMessagingWebSocketHandler webSocketHandler;
    private final StaffMessagingHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/staff-messaging")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .addInterceptors(handshakeInterceptor);
    }
}
