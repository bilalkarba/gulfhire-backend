package com.gulfhire.chat.entity;

import java.util.Optional;

/**
 * Kinds of files a chat message can carry. Mirrored by the frontend so the
 * UI can render images inline and documents as downloadable cards.
 */
public enum AttachmentType {
    PDF,
    DOCX,
    IMAGE;

    /**
     * Maps a file extension (e.g. {@code "pdf"}, {@code "docx"}) to its
     * attachment type. Case-insensitive; returns empty for unsupported types.
     */
    public static Optional<AttachmentType> fromExtension(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        return switch (extension.toLowerCase()) {
            case "pdf" -> Optional.of(PDF);
            case "doc", "docx" -> Optional.of(DOCX);
            case "jpg", "jpeg", "png", "webp", "gif" -> Optional.of(IMAGE);
            default -> Optional.empty();
        };
    }
}
