package com.gulfhire.worker.service;

import com.gulfhire.worker.dto.WorkerRequest;
import com.gulfhire.worker.dto.WorkerResponse;
import com.gulfhire.worker.dto.WorkerUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface WorkerService {
    WorkerResponse createWorker(WorkerRequest request, UUID userId);
    WorkerResponse getWorkerById(UUID id);
    WorkerResponse getWorkerByUserId(UUID userId);
    WorkerResponse updateWorker(UUID userId, WorkerUpdateRequest request);
    List<WorkerResponse> getAllWorkers();
    boolean existsByUserId(UUID userId);
}
