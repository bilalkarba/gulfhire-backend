package com.gulfhire.company.service;

import com.gulfhire.company.dto.CompanyRequest;
import com.gulfhire.company.dto.CompanyResponse;
import com.gulfhire.company.dto.CompanyUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface CompanyService {
    CompanyResponse createCompany(CompanyRequest request, UUID userId);
    CompanyResponse getCompanyById(UUID id);
    CompanyResponse getCompanyByUserId(UUID userId);
    CompanyResponse updateCompany(UUID userId, CompanyUpdateRequest request);
    List<CompanyResponse> getAllCompanies();
    boolean existsByUserId(UUID userId);
}
