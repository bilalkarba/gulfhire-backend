package com.gulfhire.job.repository;

import com.gulfhire.job.entity.Job;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    @EntityGraph(attributePaths = "company")
    List<Job> findByCompanyId(UUID companyId);

    @EntityGraph(attributePaths = "company")
    List<Job> findByActiveTrue();

    @Override
    @EntityGraph(attributePaths = "company")
    List<Job> findAll();
}
