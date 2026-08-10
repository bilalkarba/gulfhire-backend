package com.gulfhire.chat.service;

import com.gulfhire.chat.dto.ConversationResponse;
import com.gulfhire.chat.dto.CreateConversationRequest;
import com.gulfhire.chat.dto.MessageResponse;
import com.gulfhire.chat.dto.SendMessageRequest;
import com.gulfhire.chat.dto.UpdateMessageRequest;
import com.gulfhire.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    /**
     * Creates (or returns the existing) conversation between a worker and a company
     * for a specific job. Requires an ACCEPTED application for the job.
     */
    ConversationResponse createConversation(User currentUser, CreateConversationRequest request);

    List<ConversationResponse> getMyConversations(UUID userId);

    ConversationResponse getConversation(UUID conversationId, UUID userId);

    List<MessageResponse> getMessages(UUID conversationId, UUID userId);

    MessageResponse sendMessage(UUID conversationId, UUID userId, SendMessageRequest request);

    /**
     * Edits the content of a message. Only the sender may edit, and only
     * messages that are not soft-deleted. Returns the updated message.
     */
    MessageResponse editMessage(UUID messageId, UUID userId, UpdateMessageRequest request);

    /**
     * Soft-deletes a message: content is cleared, {@code deleted} is set to
     * true, and the row is kept so message history stays intact. Only the
     * sender may delete. Idempotent — deleting an already-deleted message
     * returns it unchanged.
     */
    MessageResponse deleteMessage(UUID messageId, UUID userId);

    MessageResponse markMessageRead(UUID messageId, UUID userId);

    /** True if the user is a participant of the conversation. */
    boolean isParticipant(UUID conversationId, UUID userId);
}
