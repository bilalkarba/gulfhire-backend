package com.gulfhire.chat.dto;

import java.time.Instant;
import java.util.UUID;

/** Broadcast on the conversation topic when a participant reads messages. */
public record ReadReceiptEvent(UUID conversationId, UUID readerId, Instant readAt) {
}
