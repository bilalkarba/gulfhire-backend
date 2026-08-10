package com.gulfhire.chat.service;

import com.gulfhire.chat.dto.PresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which users are currently connected to the chat WebSocket and
 * broadcasts presence changes on the global {@code /topic/presence} topic.
 *
 * <p>Multi-tab safe: presence is session-count based, so a user stays ONLINE
 * while at least one browser tab holds an open session and only goes OFFLINE
 * when the last session closes. The time of that final disconnect is stored as
 * {@code lastSeen} so clients can show "Last seen …".</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    public static final String PRESENCE_TOPIC = "/topic/presence";

    /** Number of open WebSocket sessions per user — online iff &gt; 0. */
    private final ConcurrentHashMap<UUID, Integer> sessionCounts = new ConcurrentHashMap<>();

    /** When each user was last seen online (UTC, set when their last session closes). */
    private final ConcurrentHashMap<UUID, Instant> lastSeenByUser = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;

    /** Registers a new session for the user; broadcasts ONLINE on the 0→1 transition. */
    public void userConnected(UUID userId) {
        boolean wasOnline = isOnline(userId);
        sessionCounts.merge(userId, 1, Integer::sum);
        if (!wasOnline) {
            broadcast(userId, true, null);
        }
    }

    /** Removes one session; broadcasts OFFLINE only when the last session closes. */
    public void userDisconnected(UUID userId) {
        // Decrement the counter; a null result means the mapping was removed
        // (count reached 0) — the user is fully offline.
        Integer remaining = sessionCounts.computeIfPresent(userId, (id, count) -> count <= 1 ? null : count - 1);
        if (remaining == null) {
            Instant lastSeen = Instant.now();
            lastSeenByUser.put(userId, lastSeen);
            broadcast(userId, false, lastSeen);
        }
    }

    /** True when the user has at least one open WebSocket session. */
    public boolean isOnline(UUID userId) {
        return sessionCounts.containsKey(userId);
    }

    /** The last time the user was seen online (UTC instant, null while online). */
    public Instant getLastSeen(UUID userId) {
        return lastSeenByUser.get(userId);
    }

    private void broadcast(UUID userId, boolean online, Instant lastSeen) {
        if (online) {
            log.info("PRESENCE ONLINE userId={}", userId);
        } else {
            log.info("PRESENCE OFFLINE userId={}", userId);
        }
        messagingTemplate.convertAndSend(PRESENCE_TOPIC, new PresenceEvent("PRESENCE", userId, online, lastSeen));
    }
}
