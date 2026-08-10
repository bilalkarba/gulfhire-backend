package com.gulfhire.job.controller;

import com.gulfhire.job.dto.JobCountryCount;
import com.gulfhire.job.dto.JobResponse;
import com.gulfhire.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public job board — no authentication required (see SecurityConfig:
 * {@code /api/public/**} is {@code permitAll}).
 *
 * <p>Only ACTIVE jobs are exposed, and {@link JobResponse} deliberately omits
 * any sensitive company contact details. Guests can browse and read jobs, but
 * must sign in before applying.
 */
@RestController
@RequestMapping("/api/public/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final JobService jobService;

    /** Paged list of active jobs for anonymous visitors (search + country filter supported). */
    @GetMapping
    public ResponseEntity<Page<JobResponse>> getPublicJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String country,
            Pageable pageable) {
        return ResponseEntity.ok(jobService.getPublicJobs(search, country, pageable));
    }

    /** Country facet for the browse filters (active jobs only). */
    @GetMapping("/countries")
    public ResponseEntity<List<JobCountryCount>> getPublicJobCountries() {
        return ResponseEntity.ok(jobService.getJobCountries());
    }

    /** Returns a single job, but only if it exists AND is still active. */
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getPublicJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getPublicJobById(id));
    }
}
