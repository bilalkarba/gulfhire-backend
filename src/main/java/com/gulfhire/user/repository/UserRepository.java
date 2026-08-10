package com.gulfhire.user.repository;

import com.gulfhire.common.constants.Role;
import com.gulfhire.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    /** Admin user management — optional search (name/email/phone) and role filter. */
    @Query("""
            SELECT u FROM User u
            WHERE (:search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')
              AND (:role IS NULL OR u.role = :role)
            """)
    Page<User> searchUsers(@Param("search") String search, @Param("role") Role role, Pageable pageable);
}
