package com.gulfhire.admin.controller;

import com.gulfhire.application.repository.JobApplicationRepository;
import com.gulfhire.common.constants.Role;
import com.gulfhire.company.dto.CompanyResponse;
import com.gulfhire.company.entity.Company;
import com.gulfhire.company.mapper.CompanyMapper;
import com.gulfhire.company.repository.CompanyRepository;
import com.gulfhire.job.dto.JobResponse;
import com.gulfhire.job.repository.JobRepository;
import com.gulfhire.job.service.JobService;
import com.gulfhire.user.dto.UserResponse;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import com.gulfhire.worker.dto.WorkerResponse;
import com.gulfhire.worker.entity.Worker;
import com.gulfhire.worker.repository.WorkerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobService jobService;
    private final CompanyMapper companyMapper;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = Map.of(
                "totalUsers", userRepository.count(),
                "totalWorkers", workerRepository.count(),
                "totalCompanies", companyRepository.count(),
                "totalJobs", jobRepository.count(),
                "totalApplications", applicationRepository.count()
        );
        return ResponseEntity.ok(stats);
    }

    /** Paged users with optional search (name/email/phone) and role filter. */
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            Pageable pageable) {
        Page<User> users = userRepository.searchUsers(escapeLike(search), role, defaultSort(pageable));
        Page<UserResponse> response = users.map(user -> UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build());
        return ResponseEntity.ok(response);
    }

    /** Paged worker profiles with optional search across name/email/profession/country. */
    @GetMapping("/workers")
    public ResponseEntity<Page<WorkerResponse>> getAllWorkers(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<Worker> workers = workerRepository.searchWorkers(escapeLike(search), defaultSort(pageable));
        return ResponseEntity.ok(workers.map(this::toWorkerResponse));
    }

    /** Paged companies with optional search and verification filter. */
    @GetMapping("/companies")
    public ResponseEntity<Page<CompanyResponse>> getAllCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean verified,
            Pageable pageable) {
        Page<Company> companies = companyRepository.searchCompanies(escapeLike(search), verified, defaultSort(pageable));
        return ResponseEntity.ok(companies.map(companyMapper::toCompanyResponse));
    }

    /** Paged jobs (active and inactive) with optional search and status filter. */
    @GetMapping("/jobs")
    public ResponseEntity<Page<JobResponse>> getAllJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(jobService.getAllJobs(search, active, pageable));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        // Cascade delete: remove related Worker profile and its applications
        workerRepository.findByUserId(user.getId()).ifPresent(worker -> {
            applicationRepository.deleteAll(applicationRepository.findByWorkerId(worker.getId()));
            workerRepository.delete(worker);
        });

        // Cascade delete: remove related Company profile, its jobs, and their applications
        companyRepository.findByUserId(user.getId()).ifPresent(company -> {
            jobRepository.findByCompanyId(company.getId()).forEach(job -> {
                applicationRepository.deleteAll(applicationRepository.findByJobCompanyId(company.getId()));
            });
            jobRepository.findByCompanyId(company.getId()).forEach(job -> jobRepository.delete(job));
            companyRepository.delete(company);
        });

        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    /** Escapes LIKE wildcards so user input is matched literally. */
    private static String escapeLike(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Newest first unless the caller explicitly requested a sort. */
    private Pageable defaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private WorkerResponse toWorkerResponse(Worker worker) {
        User user = worker.getUser();
        return WorkerResponse.builder()
                .id(worker.getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profession(worker.getProfession())
                .experienceYears(worker.getExperienceYears())
                .currentCountry(worker.getCurrentCountry())
                .expectedSalary(worker.getExpectedSalary())
                .about(worker.getAbout())
                .profilePictureUrl(worker.getProfilePictureUrl())
                .cvUrl(worker.getCvUrl())
                .videoCvUrl(worker.getVideoCvUrl())
                .verified(worker.getVerified())
                .createdAt(worker.getCreatedAt())
                .updatedAt(worker.getUpdatedAt())
                .build();
    }
}
