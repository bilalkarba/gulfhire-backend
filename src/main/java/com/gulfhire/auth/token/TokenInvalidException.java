package com.gulfhire.auth.token;

/**
 * Thrown when a one-time token (password reset / email verification) is
 * missing, invalid, expired, or already used. Mapped to HTTP 400 by
 * {@link com.gulfhire.common.exception.GlobalExceptionHandler} — deliberately
 * distinct from {@link IllegalArgumentException} (which the auth controller
 * maps to 409 for registration conflicts).
 */
public class TokenInvalidException extends RuntimeException {

    public TokenInvalidException(String message) {
        super(message);
    }
}
