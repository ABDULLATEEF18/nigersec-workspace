package com.nigersec.intelligence_backend.fraud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nigersec.intelligence_backend.fraud.dto.TransactionScoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class FraudRealtimeNotificationService {

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public void registerSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public void notifyFraudEvent(UUID institutionId, TransactionScoreResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "fraud.alert");
        payload.put("institutionId", institutionId != null ? institutionId.toString() : "demo-institution");
        payload.put("transactionId", response.getTransactionId());
        payload.put("riskLevel", response.getRiskLevel());
        payload.put("decision", response.getDecision());
        payload.put("recommendation", response.getRecommendation());
        payload.put("riskScore", response.getRiskScore());
        payload.put("timestamp", Instant.now().toString());
        broadcast(payload);
    }

    public void broadcast(Object payload) {
        String message;
        try {
            message = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            message = "{\"event\":\"error\",\"message\":\"Unable to serialize notification\"}";
        }

        List<WebSocketSession> deadSessions = new ArrayList<>();
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                deadSessions.add(session);
                continue;
            }
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                deadSessions.add(session);
            }
        }
        sessions.removeAll(deadSessions);
    }
}
