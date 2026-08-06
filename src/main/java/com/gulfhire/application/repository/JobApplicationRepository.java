package com.gulfhire.application.repository;

import com.gulfhire.application.entity.ApplicationStatus;
import com.gulfhire.application.entity.JobApplication;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    @EntityGraph(attributePaths = {"worker.user", "job.company"})
    List<JobApplication> findByWorkerId(UUID workerId);

    @EntityGraph(attributePaths = {"worker.user", "job.company"})
    List<JobApplication> findByJobCompanyId(UUID companyId);

    boolean existsByWorkerIdAndJobId(UUID workerId, UUID jobId);

    /** Chat business rule: only ACCEPTED applications unlock a conversation. */
    boolean existsByWorkerIdAndJobIdAndStatus(UUID workerId, UUID jobId, ApplicationStatus status);

    @Override
    @EntityGraph(attributePaths = {"worker.user", "job.company"})
    List<JobApplication> findAll();
}
