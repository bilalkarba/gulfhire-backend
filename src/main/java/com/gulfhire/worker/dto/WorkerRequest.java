package com.gulfhire.worker.dto;

import jakarta.validation.constraints.NotBlank;
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
public class WorkerRequest {

    @NotBlank(message = "Profession is required")
    @Size(max = 100, message = "Profession must not exceed 100 characters")
    private String profession;

    @Positive(message = "Experience years must be positive")
    private Integer experienceYears;

    @NotBlank(message = "Current country is required")
    @Size(max = 100, message = "Current country must not exceed 100 characters")
    private String currentCountry;

    @Positive(message = "Expected salary must be positive")
    private Double expectedSalary;

    @NotBlank(message = "About is required")
    @Size(max = 2000, message = "About must not exceed 2000 characters")
    private String about;

    private String profilePictureUrl;

    private String cvUrl;

    private String videoCvUrl;
}
