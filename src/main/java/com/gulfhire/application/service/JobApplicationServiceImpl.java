package com.gulfhire.application.service;

import com.gulfhire.application.dto.ApplicationRequest;
import com.gulfhire.application.dto.ApplicationResponse;
import com.gulfhire.application.dto.UpdateApplicationStatusRequest;
import com.gulfhire.application.entity.ApplicationStatus;
import com.gulfhire.application.entity.JobApplication;
import com.gulfhire.application.mapper.JobApplicationMapper;
import com.gulfhire.application.repository.JobApplicationRepository;
import com.gulfhire.email.service.EmailService;
import com.gulfhire.job.entity.Job;
import com.gulfhire.job.repository.JobRepository;
import com.gulfhire.notification.entity.NotificationType;
import com.gulfhire.notification.service.NotificationService;
import com.gulfhire.worker.entity.Worker;
import com.gulfhire.worker.repository.WorkerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;
    private final JobApplicationMapper jobApplicationMapper;
    private final NotificationService notificationService;
    private final EmailService emailService;

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
    public Page<ApplicationResponse> getMyApplications(UUID userId, Pageable pageable) {
        Worker worker = getWorkerByUserId(userId);
        return jobApplicationRepository.findByWorkerId(worker.getId(), pageable)
                .map(jobApplicationMapper::toApplicationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ApplicationStatus, Long> getMyApplicationStats(UUID userId) {
        Worker worker = getWorkerByUserId(userId);
        UUID workerId = worker.getId();
        Map<ApplicationStatus, Long> stats = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            stats.put(status, jobApplicationRepository.countByWorkerIdAndStatus(workerId, status));
        }
        return stats;
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
        ApplicationStatus previousStatus = application.getStatus();
        ApplicationStatus newStatus = request.getStatus();

        application.setStatus(newStatus);
        application = jobApplicationRepository.save(application);

        // Notify the worker only on an actual transition to a final decision.
        if (newStatus != previousStatus) {
            if (newStatus == ApplicationStatus.ACCEPTED) {
                notifyWorker(application, NotificationType.APPLICATION_ACCEPTED,
                        "Application accepted",
                        "Congratulations! Your application for \"" + application.getJob().getTitle()
                                + "\" has been accepted.");
                emailService.sendApplicationAcceptedEmail(
                        application.getWorker().getUser().getEmail(),
                        application.getWorker().getUser().getFullName(),
                        application.getJob().getTitle(),
                        companyName(application));
            } else if (newStatus == ApplicationStatus.REJECTED) {
                notifyWorker(application, NotificationType.APPLICATION_REJECTED,
                        "Application update",
                        "Unfortunately, your application for \"" + application.getJob().getTitle()
                                + "\" was not accepted this time.");
                emailService.sendApplicationRejectedEmail(
                        application.getWorker().getUser().getEmail(),
                        application.getWorker().getUser().getFullName(),
                        application.getJob().getTitle(),
                        companyName(application));
            }
        }

        return jobApplicationMapper.toApplicationResponse(application);
    }

    private String companyName(JobApplication application) {
        com.gulfhire.company.entity.Company company = application.getJob().getCompany();
        return company != null && company.getCompanyName() != null ? company.getCompanyName() : "the company";
    }

    private void notifyWorker(JobApplication application, NotificationType type, String title, String message) {
        notificationService.createNotification(application.getWorker().getUser(), title, message, type);
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
