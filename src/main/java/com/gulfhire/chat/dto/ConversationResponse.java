package com.gulfhire.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ConversationResponse {

    private UUID id;
    private UUID workerId;
    private String workerName;
    private String workerAvatarUrl;
    private UUID companyId;
    private String companyName;
    private String companyLogoUrl;
    private UUID jobId;
    private String jobTitle;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
    private LocalDateTime createdAt;
}
