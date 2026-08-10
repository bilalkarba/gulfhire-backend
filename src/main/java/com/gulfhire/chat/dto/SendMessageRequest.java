package com.gulfhire.chat.dto;

import com.gulfhire.chat.entity.AttachmentType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    /**
     * Message text. Optional when an attachment is provided — a message may
     * carry text, an attachment, or both.
     */
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String content;

    /** Cloudinary URL returned by {@code POST /api/chat/upload}. */
    @Size(max = 2048, message = "Attachment URL must not exceed 2048 characters")
    private String attachmentUrl;

    /** Kind of the attached file; derived from the file name when omitted. */
    private AttachmentType attachmentType;

    /** Original file name (used by the UI for the download label). */
    @Size(max = 255, message = "Attachment name must not exceed 255 characters")
    private String attachmentName;

    /** Attachment size in bytes. */
    @Positive(message = "Attachment size must be positive")
    private Long attachmentSize;

    /** A message must carry either text, an attachment, or both. */
    @AssertTrue(message = "Either message content or an attachment is required")
    public boolean isValid() {
        boolean hasContent = content != null && !content.isBlank();
        boolean hasAttachment = attachmentUrl != null && !attachmentUrl.isBlank();
        return hasContent || hasAttachment;
    }
}
