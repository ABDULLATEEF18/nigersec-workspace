package com.nigersec.intelligence_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nigersec.intelligence_backend.fraud.service.FraudRealtimeNotificationService;
import com.nigersec.intelligence_backend.fraud.websocket.FraudWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final FraudRealtimeNotificationService notificationService;
    private final ObjectMapper objectMapper;

    public WebSocketConfig(FraudRealtimeNotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public FraudWebSocketHandler fraudWebSocketHandler() {
        return new FraudWebSocketHandler(notificationService, objectMapper);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(fraudWebSocketHandler(), "/ws/fraud")
                .setAllowedOrigins("*");
    }
}
