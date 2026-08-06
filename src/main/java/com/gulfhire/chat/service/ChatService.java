package com.gulfhire.chat.service;

import com.gulfhire.chat.dto.ConversationResponse;
import com.gulfhire.chat.dto.CreateConversationRequest;
import com.gulfhire.chat.dto.MessageResponse;
import com.gulfhire.chat.dto.SendMessageRequest;
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

    MessageResponse markMessageRead(UUID messageId, UUID userId);

    /** True if the user is a participant of the conversation. */
    boolean isParticipant(UUID conversationId, UUID userId);
}
