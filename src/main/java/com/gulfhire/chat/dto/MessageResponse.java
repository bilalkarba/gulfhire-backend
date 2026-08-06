package com.gulfhire.chat.dto;

import com.gulfhire.common.constants.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageResponse {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String senderName;
    private Role senderRole;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
