package com.gulfhire.job.controller;

import com.gulfhire.common.constants.Role;
import com.gulfhire.company.repository.CompanyRepository;
import com.gulfhire.job.dto.JobRequest;
import com.gulfhire.job.dto.JobResponse;
import com.gulfhire.job.dto.JobUpdateRequest;
import com.gulfhire.job.service.JobService;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY', 'ADMIN')")
    public ResponseEntity<JobResponse> createJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JobRequest request) {
        User user = getCurrentUser(userDetails);
        JobResponse response = jobService.createJob(request, resolveCompanyId(user));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY', 'ADMIN')")
    public ResponseEntity<JobResponse> updateJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody JobUpdateRequest request) {
        User user = getCurrentUser(userDetails);
        JobResponse response = jobService.updateJob(id, request, resolveCompanyId(user));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY', 'ADMIN')")
    public ResponseEntity<Void> deleteJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        User user = getCurrentUser(userDetails);
        jobService.deleteJob(id, resolveCompanyId(user));
        return ResponseEntity.noContent().build();
    }

   @GetMapping
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Page<JobResponse>> getAllJobs(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String country,
        Pageable pageable) {
    return ResponseEntity.ok(jobService.getPublicJobs(search, country, pageable));
}

@GetMapping("/{id}")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<JobResponse> getJobById(@PathVariable UUID id) {
    return ResponseEntity.ok(jobService.getJobById(id));
}

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userDetails.getUsername()));
    }

    /**
     * Resolves the current user's company id. Returns {@code null} for ADMIN,
     * which signals the service to skip the ownership check (full access).
     */
    private UUID resolveCompanyId(User user) {
        if (user.getRole() == Role.COMPANY) {
            return companyRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Company profile not found for current user"))
                    .getId();
        }
        return null;
    }
}
