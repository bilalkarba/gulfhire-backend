package com.gulfhire.application.service;

import com.gulfhire.application.dto.ApplicationRequest;
import com.gulfhire.application.dto.ApplicationResponse;
import com.gulfhire.application.dto.UpdateApplicationStatusRequest;
import com.gulfhire.application.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface JobApplicationService {

    ApplicationResponse applyToJob(UUID jobId, UUID userId, ApplicationRequest request);

    Page<ApplicationResponse> getMyApplications(UUID userId, Pageable pageable);

    /** Per-status counts for the current worker's applications. */
    Map<ApplicationStatus, Long> getMyApplicationStats(UUID userId);

    List<ApplicationResponse> getCompanyApplications(UUID companyId);

    ApplicationResponse updateApplicationStatus(UUID applicationId, UUID companyId, UpdateApplicationStatusRequest request);
}
