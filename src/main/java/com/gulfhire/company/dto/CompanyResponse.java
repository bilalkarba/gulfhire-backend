package com.gulfhire.company.dto;

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
public class CompanyResponse {

    private UUID id;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String companyName;
    private String industry;
    private String website;
    private String description;
    private String logoUrl;
    private Boolean verified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
