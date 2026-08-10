package com.gulfhire.auth.token;

import com.gulfhire.common.util.TokenHasher;
import com.gulfhire.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Issues and validates one-time tokens for password reset and email
 * verification. Tokens are opaque 256-bit values, stored hashed, marked used
 * atomically on redemption (single-use), and expire after a short window.
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);
    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);

    private final AuthTokenRepository authTokenRepository;

    /** Creates a token and returns the raw value (to embed in the emailed link). */
    @Transactional
    public String createToken(User user, TokenType type) {
        String rawToken = generateRawToken();
        Duration ttl = type == TokenType.PASSWORD_RESET ? PASSWORD_RESET_TTL : VERIFICATION_TTL;

        authTokenRepository.save(AuthToken.builder()
                .token(TokenHasher.sha256Hex(rawToken))
                .type(type)
                .user(user)
                .expiryDate(LocalDateTime.now().plus(ttl))
                .used(false)
                .build());
        return rawToken;
    }

    /**
     * Validates and consumes a raw token of the given type.
     *
     * @return the token's owner
     * @throws TokenInvalidException when the token is missing, already used,
     *         expired, or of the wrong type
     */
    @Transactional
    public User consumeToken(String rawToken, TokenType expectedType) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new TokenInvalidException("Token is required");
        }
        String hash = TokenHasher.sha256Hex(rawToken.trim());
        AuthToken stored = authTokenRepository.findByToken(hash)
                .orElseThrow(() -> new TokenInvalidException("Invalid token"));

        if (stored.getType() != expectedType) {
            throw new TokenInvalidException("Invalid token");
        }
        if (stored.getUsed()) {
            throw new TokenInvalidException("Token has already been used");
        }
        if (stored.getExpiryDate().isBefore(LocalDateTime.now())) {
            authTokenRepository.delete(stored);
            throw new TokenInvalidException("Token has expired");
        }

        // Atomic single-use claim: exactly one caller may win this row.
        if (authTokenRepository.markUsedIfActive(hash) == 0) {
            throw new TokenInvalidException("Token has already been used");
        }
        return stored.getUser();
    }

    /** Invalidates all outstanding tokens for a user (e.g. after a password change). */
    @Transactional
    public void deleteByUser(User user) {
        authTokenRepository.deleteByUser(user);
    }

    /** Daily cleanup of expired tokens. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        authTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
