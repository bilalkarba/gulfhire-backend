package com.gulfhire.match.service;

import com.gulfhire.job.entity.Job;
import com.gulfhire.match.dto.RecommendedJobResponse;
import com.gulfhire.match.dto.RecommendedWorkerResponse;
import com.gulfhire.worker.entity.Worker;

import java.util.List;
import java.util.UUID;

public interface MatchService {

    List<RecommendedJobResponse> getRecommendedJobs(UUID workerId);

    /**
     * @param companyId the owning company id, or {@code null} for ADMIN (full access).
     *                  The job's ownership is validated against this value.
     */
    List<RecommendedWorkerResponse> getRecommendedWorkers(UUID jobId, UUID companyId);

    double calculateMatchScore(Worker worker, Job job);
}
