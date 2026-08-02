package com.gulfhire.application.service;

import com.gulfhire.application.dto.ApplicationRequest;
import com.gulfhire.application.dto.ApplicationResponse;
import com.gulfhire.application.dto.UpdateApplicationStatusRequest;
import com.gulfhire.application.entity.ApplicationStatus;
import com.gulfhire.application.entity.JobApplication;
import com.gulfhire.application.mapper.JobApplicationMapper;
import com.gulfhire.application.repository.JobApplicationRepository;
import com.gulfhire.job.entity.Job;
import com.gulfhire.job.repository.JobRepository;
import com.gulfhire.worker.entity.Worker;
import com.gulfhire.worker.repository.WorkerRepository;
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
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;
    private final JobApplicationMapper jobApplicationMapper;

    @Override
    public ApplicationResponse applyToJob(UUID jobId, UUID userId, ApplicationRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        Worker worker = getWorkerByUserId(userId);

        if (jobApplicationRepository.existsByWorkerIdAndJobId(worker.getId(), jobId)) {
            throw new IllegalStateException("You have already applied to this job");
        }

        String coverLetter = request != null ? request.getCoverLetter() : null;

        JobApplication application = JobApplication.builder()
                .worker(worker)
                .job(job)
                .coverLetter(coverLetter)
                .status(ApplicationStatus.PENDING)
                .build();
        application = jobApplicationRepository.save(application);
        return jobApplicationMapper.toApplicationResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(UUID userId) {
        Worker worker = getWorkerByUserId(userId);
        return jobApplicationRepository.findByWorkerId(worker.getId()).stream()
                .map(jobApplicationMapper::toApplicationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getCompanyApplications(UUID companyId) {
        if (companyId == null) {
            // ADMIN: full access to all applications
            return jobApplicationRepository.findAll().stream()
                    .map(jobApplicationMapper::toApplicationResponse)
                    .toList();
        }
        return jobApplicationRepository.findByJobCompanyId(companyId).stream()
                .map(jobApplicationMapper::toApplicationResponse)
                .toList();
    }

    @Override
    public ApplicationResponse updateApplicationStatus(UUID applicationId, UUID companyId, UpdateApplicationStatusRequest request) {
        JobApplication application = getApplicationForCompany(applicationId, companyId);
        application.setStatus(request.getStatus());
        application = jobApplicationRepository.save(application);
        return jobApplicationMapper.toApplicationResponse(application);
    }

    private Worker getWorkerByUserId(UUID userId) {
        return workerRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Worker profile not found for current user"));
    }

    /**
     * Loads an application and enforces ownership: a COMPANY may only manage
     * applications for its own jobs. A {@code null} companyId (ADMIN) skips
     * the ownership check (full access).
     */
    private JobApplication getApplicationForCompany(UUID applicationId, UUID companyId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found with id: " + applicationId));

        if (companyId != null && (application.getJob().getCompany() == null
                || !application.getJob().getCompany().getId().equals(companyId))) {
            throw new AccessDeniedException("You can only manage applications for your own jobs");
        }
        return application;
    }
}
