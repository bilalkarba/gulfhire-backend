package com.gulfhire.chat.dto;

import com.gulfhire.chat.entity.AttachmentType;
import com.gulfhire.common.constants.Role;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
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
    private String attachmentUrl;
    private AttachmentType attachmentType;
    private String attachmentName;
    private Long attachmentSize;
    /** True for soft-deleted messages — the UI shows a "message deleted" placeholder. */
    private Boolean deleted;
    /** Set when the sender edited the message content. */
    private Instant editedAt;
    /** UTC instant when the message was sent (ISO-8601 with Z). */
    private Instant createdAt;
}
