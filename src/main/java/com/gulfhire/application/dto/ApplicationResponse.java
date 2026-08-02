package com.gulfhire.application.dto;

import com.gulfhire.application.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private UUID id;
    private UUID workerId;
    private String workerName;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private String coverLetter;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
}
