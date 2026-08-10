package com.gulfhire.auth.repository;

import com.gulfhire.auth.entity.RefreshToken;
import com.gulfhire.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    /**
     * Atomically claims a single-use token: marks it revoked <em>only if</em> it is
     * still active. Returns the number of rows updated (1 = claimed by this caller,
     * 0 = already consumed by someone else). This is what makes rotation safe under
     * concurrency — a stolen token can never be redeemed twice.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.token = :hash AND rt.revoked = false")
    int revokeIfActive(@Param("hash") String hash);

    void deleteByToken(String token);

    void deleteByUser(User user);

    /** Bulk cleanup for the scheduled expired-token job. */
    void deleteByExpiryDateBefore(LocalDateTime now);
}
