package com.gulfhire.application.service;

import com.gulfhire.application.dto.ApplicationRequest;
import com.gulfhire.application.dto.ApplicationResponse;
import com.gulfhire.application.dto.UpdateApplicationStatusRequest;

import java.util.List;
import java.util.UUID;

public interface JobApplicationService {

    ApplicationResponse applyToJob(UUID jobId, UUID userId, ApplicationRequest request);

    List<ApplicationResponse> getMyApplications(UUID userId);

    List<ApplicationResponse> getCompanyApplications(UUID companyId);

    ApplicationResponse updateApplicationStatus(UUID applicationId, UUID companyId, UpdateApplicationStatusRequest request);
}
