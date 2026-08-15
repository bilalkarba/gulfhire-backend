package com.gulfhire.worker.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerUpdateRequest {

    @Size(max = 100, message = "Profession must not exceed 100 characters")
    private String profession;

    @Positive(message = "Experience years must be positive")
    private Integer experienceYears;

    @Size(max = 100, message = "Current country must not exceed 100 characters")
    private String currentCountry;

    @Positive(message = "Expected salary must be positive")
    private Double expectedSalary;

    @Size(max = 2000, message = "About must not exceed 2000 characters")
    private String about;

    private String profilePictureUrl;

    private String cvUrl;

    private String videoCvUrl;

    @Size(max = 2000, message = "Skills must not exceed 2000 characters")
    private String skills;

    @Size(max = 2000, message = "Education must not exceed 2000 characters")
    private String education;
}
