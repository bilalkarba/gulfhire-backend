package com.gulfhire.chat.entity;

import com.gulfhire.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /** Cloudinary URL of the attached file (null for plain text messages). */
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AttachmentType attachmentType;

    /** Original file name as sent by the user (used by the UI for download labels). */
    private String attachmentName;

    /** Attachment size in bytes. */
    private Long attachmentSize;

    /**
     * Soft-delete flag: the row is kept for message history, the content is
     * cleared, and the UI renders a "message deleted" placeholder.
     *
     * <p>Nullable so {@code ddl-auto=update} can add the column to a
     * pre-existing messages table (same precedent as {@code User.emailVerified});
     * the app always writes a value (@Builder.Default = false).</p>
     */
    @Column
    @Builder.Default
    private Boolean deleted = false;

    /** Set when the sender edits the message content. */
    private Instant editedAt;

    /**
     * UTC instant when the message was sent. Serialized as ISO-8601 ("…Z") so
     * clients can convert to browser-local time.
     *
     * <p>Note: the value is also assigned explicitly when a message is built
     * ({@code ChatServiceImpl}) — {@code @CreationTimestamp} only runs at DB
     * flush, and the response/broadcast must never carry a null timestamp.</p>
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
