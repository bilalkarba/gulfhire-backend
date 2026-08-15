package com.gulfhire.savedjob.repository;

import com.gulfhire.savedjob.entity.SavedJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, UUID> {

    /** The worker's saved jobs (newest first), with job + company eagerly loaded. */
    @Query("""
            SELECT s FROM SavedJob s
            JOIN FETCH s.job j
            JOIN FETCH j.company
            WHERE s.worker.id = :workerId
            """)
    Page<SavedJob> findByWorkerId(@Param("workerId") UUID workerId, Pageable pageable);

    /** Plain job ids the worker has saved — used by the UI to render the save toggle. */
    @Query("SELECT s.job.id FROM SavedJob s WHERE s.worker.id = :workerId")
    List<UUID> findJobIdsByWorkerId(@Param("workerId") UUID workerId);

    boolean existsByWorkerIdAndJobId(UUID workerId, UUID jobId);

    Optional<SavedJob> findByWorkerIdAndJobId(UUID workerId, UUID jobId);

    long countByWorkerId(UUID workerId);

    void deleteByWorkerIdAndJobId(UUID workerId, UUID jobId);
}
