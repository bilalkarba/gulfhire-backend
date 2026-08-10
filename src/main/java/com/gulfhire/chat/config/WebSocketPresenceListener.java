package com.gulfhire.chat.config;

import com.gulfhire.chat.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

/**
 * Tracks WebSocket sessions for online/offline presence. The authenticated
 * user is read from the {@link SessionConnectedEvent} — it carries the
 * {@link StompPrincipal} set by {@link JwtChannelInterceptor} on the CONNECT
 * frame. Keeping presence here (and out of the interceptor) breaks the
 * WebSocketConfig → interceptor → presence → broker-infra cycle.
 *
 * <p>Every stage is logged with {@code CONNECT / DISCONNECT / PRESENCE}
 * markers so a broken chain (missing event, null user, missing session id)
 * is visible in the server log instead of silently showing users offline.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketPresenceListener {

    private final WsSessionRegistry sessionRegistry;
    private final PresenceService presenceService;

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = event.getMessage() != null
                ? MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class)
                : null;
        String sessionId = accessor != null ? accessor.getSessionId() : null;

        // Resolve the authenticated user: prefer the event's principal, but
        // fall back to the CONNECT message header — some Spring versions only
        // expose the user through the message, not the event object.
        Principal user = event.getUser();
        if (user == null && accessor != null) {
            user = accessor.getUser();
        }

        if (!(user instanceof StompPrincipal principal)) {
            log.warn("CONNECT sessionId={} IGNORED — no user principal (eventUser={}, headerUser={})",
                    sessionId, event.getUser(), accessor != null ? accessor.getUser() : null);
            return;
        }
        UUID userId;
        try {
            userId = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            log.warn("CONNECT sessionId={} IGNORED — principal is not a user id: {}", sessionId, principal.getName());
            return;
        }

        // Without a session id the session could never be matched on disconnect,
        // which would pin the user "online" indefinitely — refuse rather than guess.
        if (sessionId == null) {
            log.warn("CONNECT userId={} IGNORED — session id missing from event message", userId);
            return;
        }

        sessionRegistry.register(sessionId, userId);
        presenceService.userConnected(userId);
        log.info("CONNECT userId={} sessionId={}", userId, sessionId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        UUID userId = sessionRegistry.remove(event.getSessionId());
        // Every closed session decrements the user's session count — PresenceService
        // marks the user OFFLINE (and records lastSeen) only when the last one closes.
        if (userId != null) {
            presenceService.userDisconnected(userId);
            log.info("DISCONNECT userId={} sessionId={}", userId, event.getSessionId());
        } else {
            log.warn("DISCONNECT sessionId={} IGNORED — session was never registered (connect event missed?)",
                    event.getSessionId());
        }
    }

}
