package com.gulfhire.worker.repository;

import com.gulfhire.worker.entity.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, UUID> {
    Optional<Worker> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);

    @Override
    @EntityGraph(attributePaths = "user")
    List<Worker> findAll();

    /** Admin worker management — optional search across the worker's profile and linked user. */
    @Query("""
            SELECT w FROM Worker w JOIN w.user u
            WHERE (:search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(w.profession) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                   OR LOWER(w.currentCountry) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')
            """)
    Page<Worker> searchWorkers(@Param("search") String search, Pageable pageable);
}
