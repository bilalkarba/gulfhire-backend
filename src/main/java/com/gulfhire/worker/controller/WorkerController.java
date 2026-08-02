package com.gulfhire.worker.controller;

import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import com.gulfhire.worker.dto.WorkerResponse;
import com.gulfhire.worker.dto.WorkerUpdateRequest;
import com.gulfhire.worker.service.WorkerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<WorkerResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        WorkerResponse response = workerService.getWorkerByUserId(user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<WorkerResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WorkerUpdateRequest request) {
        User user = getCurrentUser(userDetails);
        WorkerResponse response = workerService.updateWorker(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkerResponse> getWorkerById(@PathVariable UUID id) {
        WorkerResponse response = workerService.getWorkerById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    public ResponseEntity<List<WorkerResponse>> getAllWorkers() {
        List<WorkerResponse> workers = workerService.getAllWorkers();
        return ResponseEntity.ok(workers);
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userDetails.getUsername()));
    }
}
