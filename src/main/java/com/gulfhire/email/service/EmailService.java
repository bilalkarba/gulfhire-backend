package com.gulfhire.email.service;

/**
 * Transactional email delivery. All implementations must be
 * <em>asynchronous and fail-soft</em>: an email problem must never break the
 * business operation that triggered it (application decision, chat message,
 * registration, password reset). When mail is disabled
 * ({@code MAIL_ENABLED=false}), calls are no-ops.
 */
public interface EmailService {

    /** Sent to a worker when a company accepts their application. */
    void sendApplicationAcceptedEmail(String to, String recipientName, String jobTitle, String companyName);

    /** Sent to a worker when a company rejects their application. */
    void sendApplicationRejectedEmail(String to, String recipientName, String jobTitle, String companyName);

    /** Sent to the other participant when a new chat message arrives. */
    void sendNewMessageEmail(String to, String recipientName, String senderName, String messagePreview);

    /** Sent with a one-time password-reset link. */
    void sendPasswordResetEmail(String to, String recipientName, String resetLink);

    /** Sent with a one-time email-verification link. */
    void sendVerificationEmail(String to, String recipientName, String verificationLink);
}
