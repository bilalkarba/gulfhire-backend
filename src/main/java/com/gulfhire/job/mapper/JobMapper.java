package com.gulfhire.job.mapper;

import com.gulfhire.job.dto.JobRequest;
import com.gulfhire.job.dto.JobResponse;
import com.gulfhire.job.dto.JobUpdateRequest;
import com.gulfhire.job.entity.Job;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobResponse toJobResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getCompanyName())
                .title(job.getTitle())
                .description(job.getDescription())
                .country(job.getCountry())
                .city(job.getCity())
                .salary(job.getSalary())
                .contractType(job.getContractType())
                .requiredExperience(job.getRequiredExperience())
                .active(job.getActive())
                .expiresAt(job.getExpiresAt())
                .createdAt(job.getCreatedAt())
                .build();
    }

    public Job toJob(JobRequest request) {
        return Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .country(request.getCountry())
                .city(request.getCity())
                .salary(request.getSalary())
                .contractType(request.getContractType())
                .requiredExperience(request.getRequiredExperience())
                .expiresAt(request.getExpiresAt())
                .build();
    }

    public void updateJobFromRequest(JobUpdateRequest request, Job job) {
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCountry(request.getCountry());
        job.setCity(request.getCity());
        job.setSalary(request.getSalary());
        job.setContractType(request.getContractType());
        job.setRequiredExperience(request.getRequiredExperience());
        job.setActive(request.getActive());
        if (request.getExpiresAt() != null) {
            job.setExpiresAt(request.getExpiresAt());
        }
    }
}
