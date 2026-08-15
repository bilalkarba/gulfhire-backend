package com.gulfhire.savedjob.controller;

import com.gulfhire.job.dto.JobResponse;
import com.gulfhire.job.entity.Job;
import com.gulfhire.job.mapper.JobMapper;
import com.gulfhire.job.repository.JobRepository;
import com.gulfhire.savedjob.entity.SavedJob;
import com.gulfhire.savedjob.repository.SavedJobRepository;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/saved-jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
public class SavedJobController {

    private final SavedJobRepository savedJobRepository;
    private final WorkerRepository workerRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;

    /** The current worker's saved jobs (newest first), as full job responses. */
    @GetMapping
    public ResponseEntity<Page<JobResponse>> getMySavedJobs(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        UUID workerId = resolveWorkerId(userDetails);
        Page<JobResponse> jobs = savedJobRepository
                .findByWorkerId(workerId, defaultSort(pageable))
                .map(saved -> jobMapper.toJobResponse(saved.getJob()));
        return ResponseEntity.ok(jobs);
    }

    /** Plain ids of the saved jobs — lets the UI render the bookmark toggle without paging. */
    @GetMapping("/ids")
    public ResponseEntity<List<UUID>> getSavedJobIds(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(savedJobRepository.findJobIdsByWorkerId(resolveWorkerId(userDetails)));
    }

    /** Bookmarks a job. Idempotent — saving an already-saved job is a no-op. */
    @PostMapping("/{jobId}")
    public ResponseEntity<JobResponse> saveJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID jobId) {
        UUID workerId = resolveWorkerId(userDetails);

        if (!savedJobRepository.existsByWorkerIdAndJobId(workerId, jobId)) {
            Worker worker = workerRepository.findById(workerId)
                    .orElseThrow(() -> new EntityNotFoundException("Worker profile not found"));
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
            savedJobRepository.save(SavedJob.builder().worker(worker).job(job).build());
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
        return ResponseEntity.ok(jobMapper.toJobResponse(job));
    }

    /** Removes a bookmark. Idempotent — removing an unsaved job is a no-op. */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> removeSavedJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID jobId) {
        savedJobRepository.deleteByWorkerIdAndJobId(resolveWorkerId(userDetails), jobId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveWorkerId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userDetails.getUsername()));
        return workerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Worker profile not found for current user"))
                .getId();
    }

    private Pageable defaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "savedAt"));
    }
}
