package com.gulfhire.admin.controller;

import com.gulfhire.application.repository.JobApplicationRepository;
import com.gulfhire.company.entity.Company;
import com.gulfhire.company.repository.CompanyRepository;
import com.gulfhire.job.repository.JobRepository;
import com.gulfhire.user.dto.UserResponse;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import com.gulfhire.worker.entity.Worker;
import com.gulfhire.worker.repository.WorkerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(users);
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
}