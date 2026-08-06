package com.gulfhire.chat.dto;

import java.util.List;
import java.util.UUID;

/** Broadcast on the presence topic for a conversation. */
public record PresenceEvent(UUID conversationId, List<UUID> onlineUserIds) {
}
