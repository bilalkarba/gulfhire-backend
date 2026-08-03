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
public class RecommendedJobResponse {

    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private Double salary;
    private Integer matchScore;
}
