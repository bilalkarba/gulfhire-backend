package com.gulfhire.worker.mapper;

import com.gulfhire.worker.dto.WorkerRequest;
import com.gulfhire.worker.dto.WorkerResponse;
import com.gulfhire.worker.dto.WorkerUpdateRequest;
import com.gulfhire.worker.entity.Worker;
import org.springframework.stereotype.Component;

@Component
public class WorkerMapper {

    public WorkerResponse toWorkerResponse(Worker worker) {
        return WorkerResponse.builder()
                .id(worker.getId())
                .userId(worker.getUser().getId())
                .fullName(worker.getUser().getFullName())
                .email(worker.getUser().getEmail())
                .phone(worker.getUser().getPhone())
                .profession(worker.getProfession())
                .experienceYears(worker.getExperienceYears())
                .currentCountry(worker.getCurrentCountry())
                .expectedSalary(worker.getExpectedSalary())
                .about(worker.getAbout())
                .profilePictureUrl(worker.getProfilePictureUrl())
                .cvUrl(worker.getCvUrl())
                .videoCvUrl(worker.getVideoCvUrl())
                .verified(worker.getVerified())
                .createdAt(worker.getCreatedAt())
                .updatedAt(worker.getUpdatedAt())
                .build();
    }

    public Worker toWorker(WorkerRequest request) {
        return Worker.builder()
                .profession(request.getProfession())
                .experienceYears(request.getExperienceYears())
                .currentCountry(request.getCurrentCountry())
                .expectedSalary(request.getExpectedSalary())
                .about(request.getAbout())
                .profilePictureUrl(request.getProfilePictureUrl())
                .cvUrl(request.getCvUrl())
                .videoCvUrl(request.getVideoCvUrl())
                .build();
    }

    public void updateWorkerFromRequest(WorkerUpdateRequest request, Worker worker) {
        if (request.getProfession() != null) {
            worker.setProfession(request.getProfession());
        }
        if (request.getExperienceYears() != null) {
            worker.setExperienceYears(request.getExperienceYears());
        }
        if (request.getCurrentCountry() != null) {
            worker.setCurrentCountry(request.getCurrentCountry());
        }
        if (request.getExpectedSalary() != null) {
            worker.setExpectedSalary(request.getExpectedSalary());
        }
        if (request.getAbout() != null) {
            worker.setAbout(request.getAbout());
        }
        if (request.getProfilePictureUrl() != null) {
            worker.setProfilePictureUrl(request.getProfilePictureUrl());
        }
        if (request.getCvUrl() != null) {
            worker.setCvUrl(request.getCvUrl());
        }
        if (request.getVideoCvUrl() != null) {
            worker.setVideoCvUrl(request.getVideoCvUrl());
        }
    }
}
