package com.gulfhire.company.controller;

import com.gulfhire.company.dto.CompanyResponse;
import com.gulfhire.company.dto.CompanyUpdateRequest;
import com.gulfhire.company.service.CompanyService;
import com.gulfhire.job.dto.JobResponse;
import com.gulfhire.job.service.JobService;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final JobService jobService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<CompanyResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        CompanyResponse response = companyService.getCompanyByUserId(user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<CompanyResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyUpdateRequest request) {
        User user = getCurrentUser(userDetails);
        CompanyResponse response = companyService.updateCompany(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/jobs")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<List<JobResponse>> getMyJobs(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        UUID companyId = companyService.getCompanyByUserId(user.getId()).getId();
        return ResponseEntity.ok(jobService.getCompanyJobs(companyId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyResponse> getCompanyById(@PathVariable UUID id) {
        CompanyResponse response = companyService.getCompanyById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<CompanyResponse>> getAllCompanies() {
        List<CompanyResponse> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userDetails.getUsername()));
    }
}
