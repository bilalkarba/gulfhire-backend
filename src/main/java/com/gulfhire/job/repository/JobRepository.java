package com.gulfhire.job.repository;

import com.gulfhire.job.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    @EntityGraph(attributePaths = "company")
    List<Job> findByCompanyId(UUID companyId);

    /** Country facet for the public job board (active jobs only). */
    interface CountryCountProjection {
        String getCountry();
        Long getCount();
    }

    @Query("""
            SELECT j.country AS country, COUNT(j) AS count
            FROM Job j
            WHERE j.active = true
            GROUP BY j.country
            ORDER BY COUNT(j) DESC
            """)
    List<CountryCountProjection> countActiveByCountry();

    /** Used by the AI matching engine to score all active jobs for a worker. */
    @EntityGraph(attributePaths = "company")
    List<Job> findByActiveTrue();

    @EntityGraph(attributePaths = "company")
    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);

    @EntityGraph(attributePaths = "company")
    Page<Job> findByActiveTrue(Pageable pageable);

    /** Active jobs whose posting has expired — candidates for the expiry scheduler. */
    @EntityGraph(attributePaths = "company")
    List<Job> findByActiveTrueAndExpiresAtBefore(java.time.LocalDateTime now);

    @Override
    @EntityGraph(attributePaths = "company")
    Page<Job> findAll(Pageable pageable);

    /** Public/authenticated browse — active jobs only, with optional search + country filter. */
    @Query(value = """
            SELECT j FROM Job j JOIN FETCH j.company c
            WHERE j.active = true
              AND (:search = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(j.country) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(j.city) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')
              AND (:country = '' OR j.country = :country)
            """,
            countQuery = """
            SELECT COUNT(j) FROM Job j
            JOIN j.company c
            WHERE j.active = true
              AND (:search = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(j.country) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(j.city) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')
              AND (:country = '' OR j.country = :country)
            """)
    Page<Job> searchActiveJobs(@Param("search") String search, @Param("country") String country, Pageable pageable);

    /** Admin view — every job (active or not), with optional search + status filter. */
    @Query(value = """
            SELECT j FROM Job j JOIN FETCH j.company c
            WHERE (:search = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(j.country) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(j.city) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')
              AND (:active IS NULL OR j.active = :active)
            """,
            countQuery = """
            SELECT COUNT(j) FROM Job j
            JOIN j.company c
            WHERE (:search = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(j.country) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(j.city) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')
              AND (:active IS NULL OR j.active = :active)
            """)
    Page<Job> searchAllJobs(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
}
