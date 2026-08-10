package com.gulfhire.chat.mapper;

import com.gulfhire.chat.dto.ConversationResponse;
import com.gulfhire.chat.dto.MessageResponse;
import com.gulfhire.chat.entity.Conversation;
import com.gulfhire.chat.entity.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

@Component
public class ChatMapper {

    public MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .senderRole(message.getSender().getRole())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentType(message.getAttachmentType())
                .attachmentName(message.getAttachmentName())
                .attachmentSize(message.getAttachmentSize())
                .deleted(message.getDeleted())
                .editedAt(message.getEditedAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public ConversationResponse toConversationResponse(
            Conversation conversation,
            String lastMessage,
            Instant lastMessageAt,
            long unreadCount,
            String workerAvatarUrl,
            String companyLogoUrl) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .workerId(conversation.getWorker().getId())
                .workerName(conversation.getWorker().getFullName())
                .workerAvatarUrl(workerAvatarUrl)
                .companyId(conversation.getCompany().getId())
                .companyName(conversation.getJob().getCompany().getCompanyName())
                .companyLogoUrl(companyLogoUrl)
                .jobId(conversation.getJob().getId())
                .jobTitle(conversation.getJob().getTitle())
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unreadCount)
                // The conversation entity stores a naive server-local timestamp;
                // interpret it as the server's zone so the API is always UTC.
                // Null-guard: a freshly created conversation (@CreationTimestamp)
                // has no createdAt until the DB flush.
                .createdAt(conversation.getCreatedAt() == null
                        ? null
                        : conversation.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .build();
    }
}
