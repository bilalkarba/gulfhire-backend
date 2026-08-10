package com.gulfhire.application.controller;

import com.gulfhire.application.dto.ApplicationRequest;
import com.gulfhire.application.dto.ApplicationResponse;
import com.gulfhire.application.dto.UpdateApplicationStatusRequest;
import com.gulfhire.application.entity.ApplicationStatus;
import com.gulfhire.application.service.JobApplicationService;
import com.gulfhire.common.constants.Role;
import com.gulfhire.company.repository.CompanyRepository;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @PostMapping("/jobs/{jobId}/apply")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> applyToJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID jobId,
            @Valid @RequestBody(required = false) ApplicationRequest request) {
        User user = getCurrentUser(userDetails);
        ApplicationResponse response = jobApplicationService.applyToJob(jobId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/applications/my")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    public ResponseEntity<Page<ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(jobApplicationService.getMyApplications(user.getId(), pageable));
    }

    /** Per-status counts for the current worker's applications (summary cards). */
    @GetMapping("/applications/my/stats")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    public ResponseEntity<Map<ApplicationStatus, Long>> getMyApplicationStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(jobApplicationService.getMyApplicationStats(user.getId()));
    }

    @GetMapping("/companies/applications")
    @PreAuthorize("hasAnyRole('COMPANY', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> getCompanyApplications(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        UUID companyId = resolveCompanyId(user);
        return ResponseEntity.ok(jobApplicationService.getCompanyApplications(companyId));
    }

    @PutMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('COMPANY', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {
        User user = getCurrentUser(userDetails);
        UUID companyId = resolveCompanyId(user);
        ApplicationResponse response = jobApplicationService.updateApplicationStatus(id, companyId, request);
        return ResponseEntity.ok(response);
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userDetails.getUsername()));
    }

    /**
     * Resolves the current user's company id. Returns {@code null} for ADMIN,
     * which signals the service to skip ownership checks (full access).
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
