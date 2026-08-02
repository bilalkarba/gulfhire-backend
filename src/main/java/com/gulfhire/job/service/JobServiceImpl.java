package com.gulfhire.job.service;

import com.gulfhire.company.entity.Company;
import com.gulfhire.company.repository.CompanyRepository;
import com.gulfhire.job.dto.JobRequest;
import com.gulfhire.job.dto.JobResponse;
import com.gulfhire.job.dto.JobUpdateRequest;
import com.gulfhire.job.entity.Job;
import com.gulfhire.job.mapper.JobMapper;
import com.gulfhire.job.repository.JobRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobMapper jobMapper;

    @Override
    public JobResponse createJob(JobRequest request, UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("A company is required to create a job");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + companyId));

        Job job = jobMapper.toJob(request);
        job.setCompany(company);
        job = jobRepository.save(job);
        return jobMapper.toJobResponse(job);
    }

    @Override
    public JobResponse updateJob(UUID jobId, JobUpdateRequest request, UUID companyId) {
        Job job = getOwnedJob(jobId, companyId);
        jobMapper.updateJobFromRequest(request, job);
        job = jobRepository.save(job);
        return jobMapper.toJobResponse(job);
    }

    @Override
    public void deleteJob(UUID jobId, UUID companyId) {
        Job job = getOwnedJob(jobId, companyId);
        jobRepository.delete(job);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + id));
        return jobMapper.toJobResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobs() {
        // Only active jobs are visible on the public job board
        return jobRepository.findByActiveTrue().stream()
                .map(jobMapper::toJobResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getCompanyJobs(UUID companyId) {
        companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + companyId));
        return jobRepository.findByCompanyId(companyId).stream()
                .map(jobMapper::toJobResponse)
                .toList();
    }

    /**
     * Loads a job and enforces ownership: a COMPANY may only manage its own jobs.
     * A {@code null} companyId (ADMIN) skips the ownership check (full access).
     */
    private Job getOwnedJob(UUID jobId, UUID companyId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        if (companyId != null && (job.getCompany() == null || !job.getCompany().getId().equals(companyId))) {
            throw new AccessDeniedException("You can only manage your own jobs");
        }
        return job;
    }
}
