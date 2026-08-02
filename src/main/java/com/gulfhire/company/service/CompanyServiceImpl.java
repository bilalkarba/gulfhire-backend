package com.gulfhire.company.service;

import com.gulfhire.company.dto.CompanyRequest;
import com.gulfhire.company.dto.CompanyResponse;
import com.gulfhire.company.dto.CompanyUpdateRequest;
import com.gulfhire.company.entity.Company;
import com.gulfhire.company.mapper.CompanyMapper;
import com.gulfhire.company.repository.CompanyRepository;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanyMapper companyMapper;

    @Override
    public CompanyResponse createCompany(CompanyRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (companyRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Company profile already exists for this user");
        }

        Company company = companyMapper.toCompany(request);
        company.setUser(user);
        company = companyRepository.save(company);
        return companyMapper.toCompanyResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + id));
        return companyMapper.toCompanyResponse(company);
    }

    @Override
    public CompanyResponse getCompanyByUserId(UUID userId) {
        Company company = companyRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
                    return createMinimalCompany(user);
                });
        return companyMapper.toCompanyResponse(company);
    }

    @Override
    public CompanyResponse updateCompany(UUID userId, CompanyUpdateRequest request) {
        Company company = companyRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
                    return createMinimalCompany(user);
                });
        companyMapper.updateCompanyFromRequest(request, company);
        company = companyRepository.save(company);
        return companyMapper.toCompanyResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(companyMapper::toCompanyResponse)
                .toList();
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return companyRepository.existsByUserId(userId);
    }

    private Company createMinimalCompany(User user) {
        Company company = Company.builder()
                .user(user)
                .companyName("")
                .industry("")
                .website("")
                .description("")
                .verified(false)
                .build();
        return companyRepository.save(company);
    }
}
