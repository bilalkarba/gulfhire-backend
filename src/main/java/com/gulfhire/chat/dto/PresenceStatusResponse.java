package com.gulfhire.chat.dto;

import java.time.Instant;
import java.util.UUID;

/** Response of {@code GET /api/chat/users/{id}/presence}. */
public record PresenceStatusResponse(UUID userId, boolean online, Instant lastSeen) {
}
