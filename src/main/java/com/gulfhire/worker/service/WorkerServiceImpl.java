package com.gulfhire.worker.service;

import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import com.gulfhire.worker.dto.WorkerRequest;
import com.gulfhire.worker.dto.WorkerResponse;
import com.gulfhire.worker.dto.WorkerUpdateRequest;
import com.gulfhire.worker.entity.Worker;
import com.gulfhire.worker.mapper.WorkerMapper;
import com.gulfhire.worker.repository.WorkerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkerServiceImpl implements WorkerService {

    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final WorkerMapper workerMapper;

    @Override
    public WorkerResponse createWorker(WorkerRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (workerRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Worker profile already exists for this user");
        }

        Worker worker = workerMapper.toWorker(request);
        worker.setUser(user);
        worker = workerRepository.save(worker);
        return workerMapper.toWorkerResponse(worker);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerById(UUID id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Worker not found with id: " + id));
        return workerMapper.toWorkerResponse(worker);
    }

    @Override
    public WorkerResponse getWorkerByUserId(UUID userId) {
        Worker worker = workerRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
                    return createMinimalWorker(user);
                });
        return workerMapper.toWorkerResponse(worker);
    }

    @Override
    public WorkerResponse updateWorker(UUID userId, WorkerUpdateRequest request) {
        Worker worker = workerRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
                    return createMinimalWorker(user);
                });
        workerMapper.updateWorkerFromRequest(request, worker);
        worker = workerRepository.save(worker);
        return workerMapper.toWorkerResponse(worker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerResponse> getAllWorkers() {
        return workerRepository.findAll().stream()
                .map(workerMapper::toWorkerResponse)
                .toList();
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return workerRepository.existsByUserId(userId);
    }

    private Worker createMinimalWorker(User user) {
        Worker worker = Worker.builder()
                .user(user)
                .profession("")
                .experienceYears(0)
                .currentCountry("")
                .expectedSalary(0.0)
                .about("")
                .verified(false)
                .build();
        return workerRepository.save(worker);
    }
}
