package com.gulfhire.match.service;

import com.gulfhire.job.entity.Job;
import com.gulfhire.job.repository.JobRepository;
import com.gulfhire.match.dto.RecommendedJobResponse;
import com.gulfhire.match.dto.RecommendedWorkerResponse;
import com.gulfhire.worker.entity.Worker;
import com.gulfhire.worker.repository.WorkerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchServiceImpl implements MatchService {

    private static final double PROFESSION_WEIGHT = 40.0;
    private static final double EXPERIENCE_WEIGHT = 25.0;
    private static final double COUNTRY_WEIGHT = 20.0;
    private static final double SALARY_WEIGHT = 15.0;

    private final WorkerRepository workerRepository;
    private final JobRepository jobRepository;

    @Override
    public List<RecommendedJobResponse> getRecommendedJobs(UUID workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new EntityNotFoundException("Worker not found with id: " + workerId));

        return jobRepository.findByActiveTrue().stream()
                .map(job -> toRecommendedJobResponse(job, calculateMatchScore(worker, job)))
                .sorted(Comparator.comparing(RecommendedJobResponse::getMatchScore).reversed())
                .toList();
    }

    @Override
    public List<RecommendedWorkerResponse> getRecommendedWorkers(UUID jobId, UUID companyId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        if (companyId != null && (job.getCompany() == null || !job.getCompany().getId().equals(companyId))) {
            throw new AccessDeniedException("You can only view matches for your own jobs");
        }

        return workerRepository.findAll().stream()
                .map(worker -> toRecommendedWorkerResponse(worker, calculateMatchScore(worker, job)))
                .sorted(Comparator.comparing(RecommendedWorkerResponse::getMatchScore).reversed())
                .toList();
    }

    @Override
    public double calculateMatchScore(Worker worker, Job job) {
        double score = 0.0;
        score += professionScore(worker, job);
        score += experienceScore(worker, job);
        score += countryScore(worker, job);
        score += salaryScore(worker, job);
        return score;
    }

    /**
     * Profession Match (+40): job title contains the worker profession,
     * or the profession equals the job title (case-insensitive).
     */
    private double professionScore(Worker worker, Job job) {
        String profession = worker.getProfession();
        String title = job.getTitle();

        if (isBlank(profession) || isBlank(title)) {
            return 0.0;
        }

        String normalizedProfession = profession.trim().toLowerCase();
        String normalizedTitle = title.trim().toLowerCase();

        if (normalizedTitle.contains(normalizedProfession) || normalizedProfession.equals(normalizedTitle)) {
            return PROFESSION_WEIGHT;
        }
        return 0.0;
    }

    /**
     * Experience Match (+25): worker experience &gt;= required experience.
     * Otherwise proportional: (workerExperience / requiredExperience) * 25.
     */
    private double experienceScore(Worker worker, Job job) {
        int workerYears = worker.getExperienceYears() != null ? worker.getExperienceYears() : 0;
        int requiredYears = job.getRequiredExperience() != null ? job.getRequiredExperience() : 0;

        if (requiredYears <= 0) {
            return EXPERIENCE_WEIGHT;
        }
        if (workerYears >= requiredYears) {
            return EXPERIENCE_WEIGHT;
        }
        return (double) workerYears / requiredYears * EXPERIENCE_WEIGHT;
    }

    /**
     * Country Match (+20): worker's current country equals the job country
     * (case-insensitive). Unset values never match.
     */
    private double countryScore(Worker worker, Job job) {
        String workerCountry = worker.getCurrentCountry();
        String jobCountry = job.getCountry();

        if (isBlank(workerCountry) || isBlank(jobCountry)) {
            return 0.0;
        }
        if (workerCountry.trim().equalsIgnoreCase(jobCountry.trim())) {
            return COUNTRY_WEIGHT;
        }
        return 0.0;
    }

    /**
     * Salary Match (+15): worker expected salary &lt;= job salary.
     */
    private double salaryScore(Worker worker, Job job) {
        Double expectedSalary = worker.getExpectedSalary();
        Double jobSalary = job.getSalary();

        if (expectedSalary == null || jobSalary == null) {
            return 0.0;
        }
        if (expectedSalary <= jobSalary) {
            return SALARY_WEIGHT;
        }
        return 0.0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private RecommendedJobResponse toRecommendedJobResponse(Job job, double score) {
        return RecommendedJobResponse.builder()
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .companyName(job.getCompany().getCompanyName())
                .salary(job.getSalary())
                .matchScore(roundScore(score))
                .build();
    }

    private RecommendedWorkerResponse toRecommendedWorkerResponse(Worker worker, double score) {
        return RecommendedWorkerResponse.builder()
                .workerId(worker.getId())
                .workerName(worker.getUser().getFullName())
                .profession(worker.getProfession())
                .experienceYears(worker.getExperienceYears())
                .matchScore(roundScore(score))
                .build();
    }

    private Integer roundScore(double score) {
        return (int) Math.round(score);
    }
}
