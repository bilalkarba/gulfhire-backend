package com.gulfhire.job.service;

import com.gulfhire.job.dto.JobCountryCount;
import com.gulfhire.job.dto.JobRequest;
import com.gulfhire.job.dto.JobResponse;
import com.gulfhire.job.dto.JobUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface JobService {

    JobResponse createJob(JobRequest request, UUID companyId);

    JobResponse updateJob(UUID jobId, JobUpdateRequest request, UUID companyId);

    void deleteJob(UUID jobId, UUID companyId);

    JobResponse getJobById(UUID id);

    /** Public variant: returns the job only if it exists AND is active. */
    JobResponse getPublicJobById(UUID id);

    /** Active jobs only — used by the public board and the authenticated browse page. */
    Page<JobResponse> getPublicJobs(String search, String country, Pageable pageable);

    /** Country facet (active jobs only) for the browse filters. */
    List<JobCountryCount> getJobCountries();

    /** Admin view — every job (active or inactive), with optional search + status filter. */
    Page<JobResponse> getAllJobs(String search, Boolean active, Pageable pageable);

    /** Jobs posted by a single company (used by the company dashboard). */
    Page<JobResponse> getCompanyJobs(UUID companyId, Pageable pageable);
}
