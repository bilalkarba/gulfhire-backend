package com.gulfhire.job.service;

import com.gulfhire.job.dto.JobRequest;
import com.gulfhire.job.dto.JobResponse;
import com.gulfhire.job.dto.JobUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface JobService {

    JobResponse createJob(JobRequest request, UUID companyId);

    JobResponse updateJob(UUID jobId, JobUpdateRequest request, UUID companyId);

    void deleteJob(UUID jobId, UUID companyId);

    JobResponse getJobById(UUID id);

    List<JobResponse> getAllJobs();

    List<JobResponse> getCompanyJobs(UUID companyId);
}
