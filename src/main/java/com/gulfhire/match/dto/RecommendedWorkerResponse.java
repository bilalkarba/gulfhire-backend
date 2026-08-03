package com.gulfhire.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedWorkerResponse {

    private UUID workerId;
    private String workerName;
    private String profession;
    private Integer experienceYears;
    private Integer matchScore;
}
