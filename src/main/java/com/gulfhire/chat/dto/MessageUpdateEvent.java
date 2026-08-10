package com.gulfhire.chat.dto;

import java.util.UUID;

/**
 * Broadcast on a conversation topic when a message is edited or soft-deleted,
 * so every participant can update their local copy in real time.
 *
 * <p>Clients receiving this event should replace their local message (matched
 * by {@code messageId}) with the attached {@link MessageResponse} — for
 * {@link Type#DELETED} the response carries {@code deleted=true} and empty
 * content, which the UI renders as a "message deleted" placeholder.</p>
 */
public record MessageUpdateEvent(UUID conversationId, UUID messageId, Type type, MessageResponse message) {

    public enum Type {
        EDITED,
        DELETED
    }
}
