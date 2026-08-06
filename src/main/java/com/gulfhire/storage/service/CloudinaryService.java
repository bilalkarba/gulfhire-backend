package com.gulfhire.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gulfhire.storage.dto.UploadResponse;
import com.gulfhire.storage.exception.UploadException;
import com.gulfhire.storage.util.FileTypeUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private static final String RESOURCE_TYPE_IMAGE = "image";
    private static final String RESOURCE_TYPE_RAW = "raw";
    private static final String RESOURCE_TYPE_VIDEO = "video";

    private final Cloudinary cloudinary;

    public UploadResponse uploadFile(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String resourceType = FileTypeUtils.resolveResourceType(originalFilename);

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", resourceType,
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            String publicId = (String) result.get("public_id");
            String format = (String) result.get("format");
            String secureUrl = (String) result.get("secure_url");

            log.info("Cloudinary upload complete - filename: {}, resolved resource_type: {}, "
                            + "stored resource_type: {}, public_id: {}, format: {}, secure_url: {}",
                    originalFilename, resourceType, result.get("resource_type"), publicId, format, secureUrl);
            log.debug("Cloudinary upload full result: {}", result);

            // PDFs are uploaded with resource_type=image (Cloudinary's recommended approach).
            // Cloudinary returns the secure_url for an image-typed PDF already ending in
            // ".pdf", which makes the CDN serve the original PDF inline
            // (Content-Type: application/pdf) instead of forcing a download. The URL is
            // stored verbatim; appending ".pdf" to a *raw*-type URL is what caused the 404.
            return UploadResponse.builder()
                    .url(secureUrl)
                    .publicId(publicId)
                    .build();

        } catch (IOException e) {
            throw new UploadException("Upload failed", e);
        }
    }

    public void deleteImage(String publicId) {
        destroy(publicId, RESOURCE_TYPE_IMAGE);
    }

    /**
     * Deletes a worker CV. CVs are uploaded with resource_type=image, but CVs
     * uploaded before that change are stored as raw; try image first and fall
     * back to raw so legacy files can still be removed.
     */
    public void deleteCv(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new EntityNotFoundException("File not found");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", RESOURCE_TYPE_IMAGE)
            );
            if (result == null || !"ok".equals(result.get("result"))) {
                result = cloudinary.uploader().destroy(
                        publicId,
                        ObjectUtils.asMap("resource_type", RESOURCE_TYPE_RAW)
                );
            }
            if (result == null || "error".equals(result.get("result"))) {
                throw new UploadException("Cloudinary delete failed");
            }
        } catch (IOException e) {
            throw new UploadException("Cloudinary delete failed", e);
        }
    }

    public void deleteVideo(String publicId) {
        destroy(publicId, RESOURCE_TYPE_VIDEO);
    }

    private void destroy(String publicId, String resourceType) {

        if (publicId == null || publicId.isBlank()) {
            throw new EntityNotFoundException("File not found");
        }

        try {

            Map<?, ?> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", resourceType
                    )
            );

            if ("error".equals(result == null ? null : result.get("result"))) {
                throw new UploadException("Cloudinary delete failed");
            }

        } catch (IOException e) {
            throw new UploadException("Cloudinary delete failed", e);
        }
    }
}
