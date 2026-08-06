package com.gulfhire.application.mapper;

import com.gulfhire.application.dto.ApplicationResponse;
import com.gulfhire.application.entity.JobApplication;
import org.springframework.stereotype.Component;

@Component
public class JobApplicationMapper {

    public ApplicationResponse toApplicationResponse(JobApplication application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .workerId(application.getWorker().getId())
                .workerName(application.getWorker().getUser().getFullName())
                .profilePictureUrl(application.getWorker().getProfilePictureUrl())
                .cvUrl(application.getWorker().getCvUrl())
                .jobId(application.getJob().getId())
                .jobTitle(application.getJob().getTitle())
                .companyName(application.getJob().getCompany().getCompanyName())
                .coverLetter(application.getCoverLetter())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .build();
    }
}
