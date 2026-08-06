package com.gulfhire.chat.dto;

/** Payload sent by a client to the typing STOMP endpoint. */
public record TypingPayload(boolean typing) {
}
