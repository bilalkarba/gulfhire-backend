package com.gulfhire.company.repository;

import com.gulfhire.company.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);

    /** Admin company management — optional search (name/owner/email/industry) and verification filter. */
    @Query("""
            SELECT c FROM Company c JOIN c.user u
            WHERE (:search = '' OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(c.industry) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')
              AND (:verified IS NULL OR c.verified = :verified)
            """)
    Page<Company> searchCompanies(@Param("search") String search, @Param("verified") Boolean verified, Pageable pageable);
}
