package com.gulfhire.storage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class StorageExceptionHandler {

    @ExceptionHandler(UploadException.class)
    public ResponseEntity<Map<String, Object>> handleUploadException(UploadException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "File too large. Maximum allowed upload size is " + formatSize(ex.getMaxUploadSize()));
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) {
            return "unknown";
        }
        if (bytes % (1024 * 1024) == 0) {
            return (bytes / (1024 * 1024)) + "MB";
        }
        return (bytes / 1024) + "KB";
    }
}
