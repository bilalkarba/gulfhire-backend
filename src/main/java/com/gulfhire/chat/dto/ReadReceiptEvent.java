package com.gulfhire.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Broadcast on the conversation topic when a participant reads messages. */
public record ReadReceiptEvent(UUID conversationId, UUID readerId, LocalDateTime readAt) {
}
