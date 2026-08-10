package com.gulfhire.auth.exception;

/**
 * Thrown when a refresh token is missing, invalid, expired, or replayed.
 * Mapped to HTTP 401 UNAUTHORIZED by {@link com.gulfhire.common.exception.GlobalExceptionHandler}.
 */
public class TokenRefreshException extends RuntimeException {

    public TokenRefreshException(String message) {
        super(message);
    }
}
