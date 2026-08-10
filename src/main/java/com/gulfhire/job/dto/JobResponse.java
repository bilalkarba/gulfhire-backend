package com.gulfhire.job.dto;

import com.gulfhire.common.constants.ContractType;
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
public class JobResponse {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private String title;
    private String description;
    private String country;
    private String city;
    private Double salary;
    private ContractType contractType;
    private Integer requiredExperience;
    private Boolean active;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
