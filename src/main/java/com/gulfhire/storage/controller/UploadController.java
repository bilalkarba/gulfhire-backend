package com.gulfhire.storage.controller;

import com.gulfhire.storage.dto.UploadResponse;
import com.gulfhire.storage.service.CloudinaryService;
import com.gulfhire.storage.util.FileTypeUtils;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import com.gulfhire.worker.entity.Worker;
import com.gulfhire.worker.repository.WorkerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class UploadController {

    private static final String PROFILE_PICTURES_FOLDER = "workers/profile-pictures";
    private static final String CV_FOLDER = "workers/cv";
    private static final String VIDEO_CV_FOLDER = "workers/video-cv";

    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;

    @PostMapping("/upload-profile-picture")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<UploadResponse> uploadProfilePicture(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        return handleUpload(userDetails, file, MediaField.PROFILE_PICTURE);
    }

    @PostMapping("/upload-cv")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<UploadResponse> uploadCv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        return handleUpload(userDetails, file, MediaField.CV);
    }

    @PostMapping("/upload-video-cv")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<UploadResponse> uploadVideoCv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        return handleUpload(userDetails, file, MediaField.VIDEO_CV);
    }

    @DeleteMapping("/profile-picture")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<Void> deleteProfilePicture(@AuthenticationPrincipal UserDetails userDetails) {
        return handleDelete(userDetails, MediaField.PROFILE_PICTURE);
    }

    @DeleteMapping("/cv")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<Void> deleteCv(@AuthenticationPrincipal UserDetails userDetails) {
        return handleDelete(userDetails, MediaField.CV);
    }

    @DeleteMapping("/video-cv")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<Void> deleteVideoCv(@AuthenticationPrincipal UserDetails userDetails) {
        return handleDelete(userDetails, MediaField.VIDEO_CV);
    }

    private ResponseEntity<UploadResponse> handleUpload(UserDetails userDetails, MultipartFile file, MediaField field) {
        validateFile(file, field.allowedExtensions);

        Worker worker = getCurrentWorker(userDetails);

        // Replace the previous file: delete it from Cloudinary before storing the new one
        String oldPublicId = field.existingPublicIdGetter.apply(worker);
        if (oldPublicId != null && !oldPublicId.isBlank()) {
            field.deleter.accept(cloudinaryService, oldPublicId);
        }

        UploadResponse response = cloudinaryService.uploadFile(file, field.folder);
        field.urlUpdater.accept(worker, response.getUrl());
        field.publicIdUpdater.accept(worker, response.getPublicId());
        workerRepository.save(worker);

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Void> handleDelete(UserDetails userDetails, MediaField field) {
        Worker worker = getCurrentWorker(userDetails);

        String publicId = field.existingPublicIdGetter.apply(worker);
        if (publicId == null || publicId.isBlank()) {
            throw new EntityNotFoundException(field.displayName + " not found");
        }

        field.deleter.accept(cloudinaryService, publicId);
        field.urlUpdater.accept(worker, null);
        field.publicIdUpdater.accept(worker, null);
        workerRepository.save(worker);

        return ResponseEntity.noContent().build();
    }

    private void validateFile(MultipartFile file, List<String> allowedExtensions) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and must not be empty");
        }
        String extension = FileTypeUtils.getExtension(file.getOriginalFilename());
        if (extension == null || !allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(
                    "Invalid file type. Allowed types: " + String.join(", ", allowedExtensions));
        }
    }

    private Worker getCurrentWorker(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userDetails.getUsername()));
        return workerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Worker profile not found for current user"));
    }

    /**
     * Maps each uploadable media type to its Cloudinary folder, allowed extensions,
     * the Worker fields that store its url/publicId, and the Cloudinary delete operation.
     */
    private enum MediaField {
        PROFILE_PICTURE(
                PROFILE_PICTURES_FOLDER,
                FileTypeUtils.IMAGE_EXTENSIONS,
                Worker::getProfilePicturePublicId,
                Worker::setProfilePictureUrl,
                Worker::setProfilePicturePublicId,
                CloudinaryService::deleteImage,
                "Profile picture"),
        CV(
                CV_FOLDER,
                FileTypeUtils.PDF_EXTENSIONS,
                Worker::getCvPublicId,
                Worker::setCvUrl,
                Worker::setCvPublicId,
                CloudinaryService::deleteRawFile,
                "CV"),
        VIDEO_CV(
                VIDEO_CV_FOLDER,
                FileTypeUtils.VIDEO_EXTENSIONS,
                Worker::getVideoCvPublicId,
                Worker::setVideoCvUrl,
                Worker::setVideoCvPublicId,
                CloudinaryService::deleteVideo,
                "Video CV");

        private final String folder;
        private final List<String> allowedExtensions;
        private final Function<Worker, String> existingPublicIdGetter;
        private final BiConsumer<Worker, String> urlUpdater;
        private final BiConsumer<Worker, String> publicIdUpdater;
        private final BiConsumer<CloudinaryService, String> deleter;
        private final String displayName;

        MediaField(String folder, List<String> allowedExtensions,
                   Function<Worker, String> existingPublicIdGetter,
                   BiConsumer<Worker, String> urlUpdater,
                   BiConsumer<Worker, String> publicIdUpdater,
                   BiConsumer<CloudinaryService, String> deleter,
                   String displayName) {
            this.folder = folder;
            this.allowedExtensions = allowedExtensions;
            this.existingPublicIdGetter = existingPublicIdGetter;
            this.urlUpdater = urlUpdater;
            this.publicIdUpdater = publicIdUpdater;
            this.deleter = deleter;
            this.displayName = displayName;
        }
    }
}
