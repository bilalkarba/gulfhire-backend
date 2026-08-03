package com.gulfhire.storage.exception;

/**
 * Thrown when an upload to Cloudinary fails.
 * Handled by {@link StorageExceptionHandler} and returned as HTTP 500.
 */
public class UploadException extends RuntimeException {

    public UploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public UploadException(String message) {
        super(message);
    }
}
