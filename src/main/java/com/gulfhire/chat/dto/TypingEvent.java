package com.gulfhire.chat.dto;

import java.util.UUID;

/** Broadcast on the conversation topic when a participant is typing. */
public record TypingEvent(UUID conversationId, UUID userId, boolean typing) {
}
