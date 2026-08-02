package com.gulfhire.company.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyUpdateRequest {

    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String companyName;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private String logoUrl;
}
