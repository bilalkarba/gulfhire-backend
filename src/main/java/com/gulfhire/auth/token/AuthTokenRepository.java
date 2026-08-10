package com.gulfhire.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    Optional<AuthToken> findByToken(String token);

    void deleteByUser(com.gulfhire.user.entity.User user);

    /** Bulk cleanup for the scheduled expired-token job. */
    void deleteByExpiryDateBefore(LocalDateTime now);

    /** Atomic single-use claim — prevents double redemption under concurrency. */
    @Modifying
    @Query("UPDATE AuthToken t SET t.used = true WHERE t.token = :hash AND t.used = false")
    int markUsedIfActive(@Param("hash") String hash);
}
