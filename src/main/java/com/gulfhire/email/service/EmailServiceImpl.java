package com.gulfhire.email.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine emailTemplateEngine;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@gulfhire.com}")
    private String fromAddress;

    @Async("mailExecutor")
    @Override
    public void sendApplicationAcceptedEmail(String to, String recipientName, String jobTitle, String companyName) {
        send(to, "Your application was accepted!",
                "application-accepted",
                Map.of("recipientName", recipientName, "jobTitle", jobTitle, "companyName", companyName));
    }

    @Async("mailExecutor")
    @Override
    public void sendApplicationRejectedEmail(String to, String recipientName, String jobTitle, String companyName) {
        send(to, "Update on your application",
                "application-rejected",
                Map.of("recipientName", recipientName, "jobTitle", jobTitle, "companyName", companyName));
    }

    @Async("mailExecutor")
    @Override
    public void sendNewMessageEmail(String to, String recipientName, String senderName, String messagePreview) {
        send(to, "New message from " + senderName,
                "new-message",
                Map.of("recipientName", recipientName, "senderName", senderName, "messagePreview", messagePreview));
    }

    @Async("mailExecutor")
    @Override
    public void sendPasswordResetEmail(String to, String recipientName, String resetLink) {
        send(to, "Reset your GulfHire password",
                "password-reset",
                Map.of("recipientName", recipientName, "resetLink", resetLink));
    }

    @Async("mailExecutor")
    @Override
    public void sendVerificationEmail(String to, String recipientName, String verificationLink) {
        send(to, "Verify your GulfHire email",
                "email-verification",
                Map.of("recipientName", recipientName, "verificationLink", verificationLink));
    }

    /**
     * Renders the template and sends the message. Fail-soft: any problem
     * (SMTP down, bad credentials, template error) is logged and swallowed so
     * the caller's transaction is never affected.
     */
    private void send(String to, String subject, String templateName, Map<String, Object> model) {
        if (!mailEnabled) {
            log.debug("Mail disabled (MAIL_ENABLED=false) — skipping '{}' to {}", subject, to);
            return;
        }
        try {
            Context context = new Context(DEFAULT_LOCALE, model);
            String html = emailTemplateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Sent '{}' to {}", subject, to);
        } catch (Exception e) {
            log.warn("Failed to send '{}' to {}: {}", subject, to, e.getMessage());
        }
    }
}
