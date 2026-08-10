package com.gulfhire.auth.service;

import com.gulfhire.auth.dto.TokenRefreshResponse;
import com.gulfhire.auth.entity.RefreshToken;
import com.gulfhire.auth.exception.TokenRefreshException;
import com.gulfhire.auth.repository.RefreshTokenRepository;
import com.gulfhire.security.jwt.JwtService;
import com.gulfhire.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issuance, rotation, and revocation of opaque refresh tokens.
 *
 * <p>Security model (Spring Security / OAuth2 best practices):</p>
 * <ul>
 *   <li><b>Opaque tokens</b> — 256-bit cryptographically-random values, never JWTs.</li>
 *   <li><b>Hashed at rest</b> — only the SHA-256 hash is persisted, so a DB leak
 *       cannot be used to mint access tokens.</li>
 *   <li><b>Rotation</b> — every refresh revokes the presented token and issues a new one,
 *       so a stolen token is only usable once.</li>
 *   <li><b>Replay detection</b> — presenting an already-rotated (revoked) token is treated
 *       as credential theft: the entire token family for that user is revoked.</li>
 *   <li><b>Expiry</b> — tokens expire after 7 days (configurable via
 *       {@code security.jwt.refresh-token-expiration}).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${security.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Issues a new refresh token for the given user and returns the raw token
     * (shown to the client exactly once; only its hash is persisted).
     */
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateRawToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(hash(rawToken))
                .expiryDate(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)))
                .user(user)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Throws if the token has expired. Deletes expired tokens so they cannot be reused.
     */
    @Transactional
    public void verifyExpiration(RefreshToken refreshToken) {
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token has expired. Please sign in again.");
        }
    }

    /** Deletes every refresh token belonging to the user (e.g. on logout / password change). */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    /** Deletes a single refresh token by its raw value (logout of one session). */
    @Transactional
    public void deleteByToken(String rawToken) {
        refreshTokenRepository.deleteByToken(hash(rawToken));
    }

    /**
     * Exchanges a valid refresh token for a fresh access token + a rotated refresh token.
     *
     * @return the new token pair
     * @throws TokenRefreshException when the token is missing, invalid, expired, or replayed
     */
    @Transactional
    public TokenRefreshResponse refreshAccessToken(String rawToken) {
        String hash = hash(rawToken);

        RefreshToken stored = refreshTokenRepository
                .findByToken(hash)
                .orElseThrow(() -> new TokenRefreshException("Refresh token is invalid. Please sign in again."));

        // Atomic single-use claim: exactly one caller may win this row.
        // If the conditional update affects 0 rows, the token was already consumed
        // (replay/theft, or a client retry after a lost response).
        if (refreshTokenRepository.revokeIfActive(hash) == 0) {
            // Revoke the whole family — the standard response to suspected theft.
            // Note: this also logs out the user's other legitimate sessions; that
            // tradeoff is accepted in exchange for stopping a stolen token's reuse.
            deleteByUser(stored.getUser());
            throw new TokenRefreshException(
                    "Refresh token has already been used. All sessions were revoked — please sign in again.");
        }

        verifyExpiration(stored);

        User user = stored.getUser();

        // Rotation: the presented token is single-use (already revoked above) —
        // issue a fresh replacement pair.
        String newRefreshToken = createRefreshToken(user);
        String newAccessToken = jwtService.generateToken(user);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * Daily cleanup of expired tokens (runs at 03:00 server time).
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex digest — the only representation of a refresh token ever stored. */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
