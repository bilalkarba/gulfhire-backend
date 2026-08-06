package com.gulfhire.chat.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WsSessionRegistry {

    private final Map<String, UUID> sessions = new ConcurrentHashMap<>();

    public void register(String sessionId, UUID userId) {
        sessions.put(sessionId, userId);
    }

    public UUID remove(String sessionId) {
        return sessions.remove(sessionId);
    }

    /** True when the user still has at least one open WebSocket session. */
    public boolean hasSessionFor(UUID userId) {
        return sessions.containsValue(userId);
    }
}
