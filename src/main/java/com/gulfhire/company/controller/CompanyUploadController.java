package com.gulfhire.company.controller;

import com.gulfhire.company.entity.Company;
import com.gulfhire.company.repository.CompanyRepository;
import com.gulfhire.storage.dto.UploadResponse;
import com.gulfhire.storage.service.CloudinaryService;
import com.gulfhire.storage.util.FileTypeUtils;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Company media uploads (logo). Mirrors the worker upload endpoints in
 * {@code UploadController}: uploads go to Cloudinary and the resulting URL
 * replaces the previous logo.
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyUploadController {

    private static final String LOGO_FOLDER = "companies/logos";

    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    /** Uploads a new company logo (jpg/jpeg/png/webp). Replaces any previous logo. */
    @PostMapping("/upload-logo")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<UploadResponse> uploadLogo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        validateImage(file);

        Company company = getCurrentCompany(userDetails);

        if (company.getLogoPublicId() != null && !company.getLogoPublicId().isBlank()) {
            cloudinaryService.deleteImage(company.getLogoPublicId());
        }

        UploadResponse response = cloudinaryService.uploadFile(file, LOGO_FOLDER);
        company.setLogoUrl(response.getUrl());
        company.setLogoPublicId(response.getPublicId());
        companyRepository.save(company);

        return ResponseEntity.ok(response);
    }

    /** Deletes the current company logo. */
    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<Void> deleteLogo(@AuthenticationPrincipal UserDetails userDetails) {
        Company company = getCurrentCompany(userDetails);

        if (company.getLogoPublicId() == null || company.getLogoPublicId().isBlank()) {
            throw new EntityNotFoundException("Logo not found");
        }

        cloudinaryService.deleteImage(company.getLogoPublicId());
        company.setLogoUrl(null);
        company.setLogoPublicId(null);
        companyRepository.save(company);

        return ResponseEntity.noContent().build();
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and must not be empty");
        }
        String extension = FileTypeUtils.getExtension(file.getOriginalFilename());
        if (extension == null || !FileTypeUtils.IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Invalid file type. Allowed types: " + String.join(", ", FileTypeUtils.IMAGE_EXTENSIONS));
        }
    }

    private Company getCurrentCompany(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userDetails.getUsername()));
        return companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Company profile not found for current user"));
    }
}
