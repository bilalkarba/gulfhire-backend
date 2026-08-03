package com.gulfhire.worker.repository;

import com.gulfhire.worker.entity.Worker;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, UUID> {
    Optional<Worker> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);

    @Override
    @EntityGraph(attributePaths = "user")
    List<Worker> findAll();
}
