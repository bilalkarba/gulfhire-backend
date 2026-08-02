package com.gulfhire.company.mapper;

import com.gulfhire.company.dto.CompanyRequest;
import com.gulfhire.company.dto.CompanyResponse;
import com.gulfhire.company.dto.CompanyUpdateRequest;
import com.gulfhire.company.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toCompanyResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .userId(company.getUser().getId())
                .fullName(company.getUser().getFullName())
                .email(company.getUser().getEmail())
                .phone(company.getUser().getPhone())
                .companyName(company.getCompanyName())
                .industry(company.getIndustry())
                .website(company.getWebsite())
                .description(company.getDescription())
                .logoUrl(company.getLogoUrl())
                .verified(company.getVerified())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    public Company toCompany(CompanyRequest request) {
        return Company.builder()
                .companyName(request.getCompanyName())
                .industry(request.getIndustry())
                .website(request.getWebsite())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .build();
    }

    public void updateCompanyFromRequest(CompanyUpdateRequest request, Company company) {
        if (request.getCompanyName() != null) {
            company.setCompanyName(request.getCompanyName());
        }
        if (request.getIndustry() != null) {
            company.setIndustry(request.getIndustry());
        }
        if (request.getWebsite() != null) {
            company.setWebsite(request.getWebsite());
        }
        if (request.getDescription() != null) {
            company.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            company.setLogoUrl(request.getLogoUrl());
        }
    }
}
