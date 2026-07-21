package com.nigersec.intelligence_backend.fraud.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nigersec.intelligence_backend.fraud.service.FraudRealtimeNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class FraudWebSocketHandler extends TextWebSocketHandler {

    private final FraudRealtimeNotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        notificationService.registerSession(session);
        send(session, Map.of(
                "event", "connected",
                "message", "Connected to NigerSec fraud alerts"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload.trim())) {
            send(session, Map.of("event", "pong", "message", "Realtime stream active"));
            return;
        }
        send(session, Map.of(
                "event", "subscribed",
                "message", "Fraud alerts stream subscribed"
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        notificationService.removeSession(session);
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException ignored) {
            // no-op for now
        }
    }
}
