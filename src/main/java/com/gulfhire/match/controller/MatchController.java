package com.gulfhire.match.controller;

import com.gulfhire.common.constants.Role;
import com.gulfhire.company.repository.CompanyRepository;
import com.gulfhire.match.dto.RecommendedJobResponse;
import com.gulfhire.match.dto.RecommendedWorkerResponse;
import com.gulfhire.match.service.MatchService;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import com.gulfhire.worker.repository.WorkerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final CompanyRepository companyRepository;

    @GetMapping("/recommended")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    public ResponseEntity<List<RecommendedJobResponse>> getRecommendedJobs(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        UUID workerId = workerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Worker profile not found for current user"))
                .getId();
        return ResponseEntity.ok(matchService.getRecommendedJobs(workerId));
    }

    @GetMapping("/{jobId}/matches")
    @PreAuthorize("hasAnyRole('COMPANY', 'ADMIN')")
    public ResponseEntity<List<RecommendedWorkerResponse>> getRecommendedWorkers(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID jobId) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(matchService.getRecommendedWorkers(jobId, resolveCompanyId(user)));
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
