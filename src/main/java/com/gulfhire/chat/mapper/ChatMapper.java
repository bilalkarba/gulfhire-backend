package com.gulfhire.chat.mapper;

import com.gulfhire.chat.dto.ConversationResponse;
import com.gulfhire.chat.dto.MessageResponse;
import com.gulfhire.chat.entity.Conversation;
import com.gulfhire.chat.entity.Message;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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
                .createdAt(message.getCreatedAt())
                .build();
    }

    public ConversationResponse toConversationResponse(
            Conversation conversation,
            String lastMessage,
            LocalDateTime lastMessageAt,
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
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}
