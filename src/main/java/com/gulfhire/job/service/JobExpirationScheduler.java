package com.gulfhire.job.service;

import com.gulfhire.application.entity.JobApplication;
import com.gulfhire.application.repository.JobApplicationRepository;
import com.gulfhire.job.entity.Job;
import com.gulfhire.job.repository.JobRepository;
import com.gulfhire.notification.entity.NotificationType;
import com.gulfhire.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Marks job postings as inactive once they expire and notifies every worker
 * who applied. Runs hourly; deactivation is idempotent (expired jobs are
 * already inactive on the next pass).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExpirationScheduler {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<Job> expired = jobRepository.findByActiveTrueAndExpiresAtBefore(now);
        if (expired.isEmpty()) {
            return;
        }

        for (Job job : expired) {
            job.setActive(false);
            jobRepository.save(job);

            List<JobApplication> applications = jobApplicationRepository.findByJobId(job.getId());
            for (JobApplication application : applications) {
                notificationService.createNotification(
                        application.getWorker().getUser(),
                        "Job expired",
                        "The job \"" + job.getTitle() + "\" you applied to is no longer accepting applications.",
                        NotificationType.JOB_EXPIRED);
            }

            log.info("Expired job {} ({} applicants notified)", job.getId(), applications.size());
        }
    }
}
