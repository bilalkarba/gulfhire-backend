package com.gulfhire.chat.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Broadcast on the global presence topic {@code /topic/presence} whenever a
 * user connects or disconnects.
 *
 * @param type     always {@code "PRESENCE"}
 * @param userId   the user whose status changed
 * @param online   true when the user came online, false when fully offline
 * @param lastSeen set on offline events (UTC instant when the user was last
 *                 seen online); null while online
 */
public record PresenceEvent(String type, UUID userId, boolean online, Instant lastSeen) {
}
