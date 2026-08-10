package com.gulfhire.storage.util;

import java.util.List;
import java.util.Map;

public final class FileTypeUtils {

    public static final List<String> IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");
    public static final List<String> PDF_EXTENSIONS = List.of("pdf");
    public static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mov", "avi");

    /** Extensions accepted by the chat attachment upload (PDF, DOCX, images, CV files). */
    public static final List<String> CHAT_ATTACHMENT_EXTENSIONS = List.of(
            "pdf", "doc", "docx", "jpg", "jpeg", "png", "webp", "gif");

    private static final String RESOURCE_TYPE_IMAGE = "image";
    private static final String RESOURCE_TYPE_VIDEO = "video";
    private static final String RESOURCE_TYPE_RAW = "raw";
    private static final String RESOURCE_TYPE_AUTO = "auto";

    private static final Map<String, String> RESOURCE_TYPES = Map.ofEntries(
            Map.entry("jpg", RESOURCE_TYPE_IMAGE),
            Map.entry("jpeg", RESOURCE_TYPE_IMAGE),
            Map.entry("png", RESOURCE_TYPE_IMAGE),
            Map.entry("webp", RESOURCE_TYPE_IMAGE),
            Map.entry("gif", RESOURCE_TYPE_IMAGE),
            // PDFs are uploaded with the image resource type (Cloudinary's recommended
            // approach) so the delivery URL can serve the original PDF inline with
            // Content-Type: application/pdf while keeping the .pdf extension.
            Map.entry("pdf", RESOURCE_TYPE_IMAGE),
            // Office documents are uploaded as raw files.
            Map.entry("doc", RESOURCE_TYPE_RAW),
            Map.entry("docx", RESOURCE_TYPE_RAW),
            Map.entry("mp4", RESOURCE_TYPE_VIDEO),
            Map.entry("mov", RESOURCE_TYPE_VIDEO),
            Map.entry("avi", RESOURCE_TYPE_VIDEO)
    );

    private FileTypeUtils() {
    }

    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }

    public static String resolveResourceType(String filename) {
        String extension = getExtension(filename);
        if (extension != null && RESOURCE_TYPES.containsKey(extension)) {
            return RESOURCE_TYPES.get(extension);
        }
        return RESOURCE_TYPE_AUTO;
    }
}
