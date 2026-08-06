package com.gulfhire.chat.config;

import com.gulfhire.chat.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.UUID;

/**
 * Tracks WebSocket sessions for online/offline presence. The authenticated
 * user is read from the {@link SessionConnectedEvent} — it carries the
 * {@link StompPrincipal} set by {@link JwtChannelInterceptor} on the CONNECT
 * frame. Keeping presence here (and out of the interceptor) breaks the
 * WebSocketConfig → interceptor → presence → broker-infra cycle.
 */
@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {

    private final WsSessionRegistry sessionRegistry;
    private final ChatPresenceService presenceService;

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        if (event.getUser() instanceof StompPrincipal principal) {
            UUID userId = UUID.fromString(principal.getName());
            // SessionConnectedEvent does not expose getSessionId(); read the
            // session id from the STOMP headers of the underlying message.
            // Null-guard: an unregistered session id could never be removed on
            // disconnect, which would pin the user "online" indefinitely.
            StompHeaderAccessor accessor = event.getMessage() != null
                    ? MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class)
                    : null;
            String sessionId = accessor != null ? accessor.getSessionId() : null;
            if (sessionId != null) {
                sessionRegistry.register(sessionId, userId);
                presenceService.userConnected(userId);
            }
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        UUID userId = sessionRegistry.remove(event.getSessionId());
        // A user may hold several sockets (multiple tabs) — only go offline
        // once the last session for that user has closed.
        if (userId != null && !sessionRegistry.hasSessionFor(userId)) {
            presenceService.userDisconnected(userId);
        }
    }
}
