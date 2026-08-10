package com.gulfhire.auth.token;

import com.gulfhire.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One-time, expiring token used for password reset and email verification.
 * Only the SHA-256 hash of the raw token is persisted (same security model as
 * refresh tokens) — the plaintext is shown to the user exactly once, inside
 * the emailed link.
 */
@Entity
@Table(
        name = "auth_tokens",
        indexes = {
                @Index(name = "idx_auth_token_user", columnList = "user_id"),
                @Index(name = "idx_auth_token_expiry", columnList = "expiryDate")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** SHA-256 hex hash of the opaque token (never the raw token). */
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TokenType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
