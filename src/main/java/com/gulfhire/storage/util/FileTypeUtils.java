package com.gulfhire.storage.util;

import java.util.List;
import java.util.Map;

public final class FileTypeUtils {

    public static final List<String> IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");
    public static final List<String> PDF_EXTENSIONS = List.of("pdf");
    public static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mov", "avi");

    private static final String RESOURCE_TYPE_IMAGE = "image";
    private static final String RESOURCE_TYPE_RAW = "raw";
    private static final String RESOURCE_TYPE_VIDEO = "video";
    private static final String RESOURCE_TYPE_AUTO = "auto";

    private static final Map<String, String> RESOURCE_TYPES = Map.of(
            "jpg", RESOURCE_TYPE_IMAGE,
            "jpeg", RESOURCE_TYPE_IMAGE,
            "png", RESOURCE_TYPE_IMAGE,
            "webp", RESOURCE_TYPE_IMAGE,
            "pdf", RESOURCE_TYPE_RAW,
            "mp4", RESOURCE_TYPE_VIDEO,
            "mov", RESOURCE_TYPE_VIDEO,
            "avi", RESOURCE_TYPE_VIDEO
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
