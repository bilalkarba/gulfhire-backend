package com.gulfhire.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
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
    /** UTC instant of the last message (ISO-8601 with Z). */
    private Instant lastMessageAt;
    private long unreadCount;
    /** UTC instant the conversation was created (ISO-8601 with Z). */
    private Instant createdAt;
}
