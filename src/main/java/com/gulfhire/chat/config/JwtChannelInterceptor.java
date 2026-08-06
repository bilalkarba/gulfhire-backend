package com.gulfhire.chat.config;

import com.gulfhire.chat.repository.ConversationRepository;
import com.gulfhire.security.jwt.JwtService;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Secures the STOMP layer: validates the JWT on CONNECT (setting the principal
 * used by downstream handlers and the presence listener) and blocks SUBSCRIBE
 * frames to conversations the user is not a participant of.
 *
 * <p>Deliberately depends only on leaf beans (JWT, repositories) — presence
 * bookkeeping lives in {@link WebSocketPresenceListener}, which keeps this
 * bean out of the WebSocket configuration dependency cycle.
 */
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor);
            default -> {
                // SEND frames are authorized in the message handlers (@MessageMapping).
            }
        }
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty() || authHeaders.get(0).isBlank()) {
            throw new AccessDeniedException("Missing JWT token");
        }
        String token = authHeaders.get(0);
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Invalid token"));

        // The principal (user id) rides on the CONNECT frame; the presence
        // listener picks it up from the SessionConnectedEvent that follows.
        accessor.setUser(new StompPrincipal(user.getId().toString()));
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        UUID conversationId = parseDestinationId(accessor.getDestination());
        if (conversationId == null) {
            return;
        }
        UUID userId = currentUserId(accessor);
        if (userId == null || !conversationRepository.existsParticipant(conversationId, userId)) {
            throw new AccessDeniedException("You are not a participant of this conversation");
        }
    }

    /** Extracts a conversation id from /topic/conversation/{id} or /topic/presence/{id}. */
    private UUID parseDestinationId(String destination) {
        if (destination == null) {
            return null;
        }
        String[] parts = destination.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("conversation") || parts[i].equals("presence")) {
                try {
                    return UUID.fromString(parts[i + 1]);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private UUID currentUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            return null;
        }
        try {
            return UUID.fromString(accessor.getUser().getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
