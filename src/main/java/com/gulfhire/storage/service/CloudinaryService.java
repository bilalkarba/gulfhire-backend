package com.gulfhire.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gulfhire.storage.dto.UploadResponse;
import com.gulfhire.storage.exception.UploadException;
import com.gulfhire.storage.util.FileTypeUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final String RESOURCE_TYPE_IMAGE = "image";
    private static final String RESOURCE_TYPE_RAW = "raw";
    private static final String RESOURCE_TYPE_VIDEO = "video";

    private final Cloudinary cloudinary;

    public UploadResponse uploadFile(MultipartFile file, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", FileTypeUtils.resolveResourceType(file.getOriginalFilename())
                    )
            );
            return UploadResponse.builder()
                    .url((String) result.get("url"))
                    .publicId((String) result.get("public_id"))
                    .build();
        } catch (IOException e) {
            throw new UploadException("Upload failed", e);
        }
    }

    /**
     * Deletes a previously uploaded profile picture from Cloudinary.
     */
    public void deleteImage(String publicId) {
        destroy(publicId, RESOURCE_TYPE_IMAGE);
    }

    /**
     * Deletes a previously uploaded CV (PDF) from Cloudinary.
     */
    public void deleteRawFile(String publicId) {
        destroy(publicId, RESOURCE_TYPE_RAW);
    }

    /**
     * Deletes a previously uploaded video CV from Cloudinary.
     */
    public void deleteVideo(String publicId) {
        destroy(publicId, RESOURCE_TYPE_VIDEO);
    }

    private void destroy(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            throw new EntityNotFoundException("File not found");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(
                    publicId, ObjectUtils.asMap("resource_type", resourceType));
            // destroy() does not throw for API-level failures; "error" means the delete did not happen.
            // "not found" is treated as success so the delete endpoints stay idempotent.
            if ("error".equals(result == null ? null : result.get("result"))) {
                throw new UploadException("Cloudinary delete failed");
            }
        } catch (IOException e) {
            throw new UploadException("Cloudinary delete failed", e);
        }
    }
}
