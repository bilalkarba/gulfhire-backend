package com.gulfhire.auth.service;

import com.gulfhire.auth.dto.AuthResponse;
import com.gulfhire.auth.dto.LoginRequest;
import com.gulfhire.auth.dto.RegisterRequest;
import com.gulfhire.auth.dto.TokenRefreshResponse;
import com.gulfhire.auth.service.RefreshTokenService;
import com.gulfhire.auth.token.AuthTokenService;
import com.gulfhire.auth.token.TokenType;
import com.gulfhire.common.constants.Role;
import com.gulfhire.email.service.EmailService;
import com.gulfhire.company.entity.Company;
import com.gulfhire.company.repository.CompanyRepository;
import com.gulfhire.security.jwt.JwtService;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import com.gulfhire.worker.entity.Worker;
import com.gulfhire.worker.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WorkerRepository workerRepository;
    private final CompanyRepository companyRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuthTokenService authTokenService;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public AuthResponse registerWorker(RegisterRequest request) {
        validateRegistration(request);
        User user = buildUser(request, Role.WORKER);
        user = userRepository.save(user);

        // Auto-create Worker profile linked to the User
        Worker worker = Worker.builder()
                .user(user)
                .profession("")
                .experienceYears(0)
                .currentCountry("")
                .expectedSalary(0.0)
                .about("")
                .verified(false)
                .build();
        workerRepository.save(worker);

        sendVerificationEmail(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse registerCompany(RegisterRequest request) {
        validateRegistration(request);
        User user = buildUser(request, Role.COMPANY);
        user = userRepository.save(user);

        // Auto-create Company profile linked to the User
        Company company = Company.builder()
                .user(user)
                .companyName("")
                .industry("")
                .website("")
                .description("")
                .verified(false)
                .build();
        companyRepository.save(company);

        sendVerificationEmail(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return buildAuthResponse(user);
    }

    /**
     * Exchanges a valid refresh token for a fresh access token + rotated refresh token.
     */
    public TokenRefreshResponse refreshAccessToken(String refreshToken) {
        return refreshTokenService.refreshAccessToken(refreshToken);
    }

    /**
     * Revokes the presented refresh token so it can no longer be used to mint
     * access tokens. Idempotent — a missing/already-revoked token is a no-op.
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.deleteByToken(refreshToken);
        }
    }

    /**
     * Starts the password-reset flow: issues a one-time token and emails the
     * reset link. Always returns normally (even for unknown emails) so the
     * endpoint cannot be used to probe which addresses are registered.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = authTokenService.createToken(user, TokenType.PASSWORD_RESET);
            String resetLink = frontendUrl + "/auth/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
        });
    }

    /** Completes the password-reset flow: validates the token and sets a new password. */
    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        User user = authTokenService.consumeToken(rawToken, TokenType.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Security best practice: a password change invalidates all existing sessions.
        refreshTokenService.deleteByUser(user);
        authTokenService.deleteByUser(user);
    }

    /** Resends the verification email for the given address. */
    @Transactional
    public void requestEmailVerification(String email) {
        userRepository.findByEmail(email).ifPresent(this::sendVerificationEmail);
    }

    /** Validates a verification token and marks the user's email as verified. */
    @Transactional
    public void verifyEmail(String rawToken) {
        User user = authTokenService.consumeToken(rawToken, TokenType.EMAIL_VERIFICATION);
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }
    }

    private void sendVerificationEmail(User user) {
        String rawToken = authTokenService.createToken(user, TokenType.EMAIL_VERIFICATION);
        String verificationLink = frontendUrl + "/auth/verify-email?token=" + rawToken;
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationLink);
    }

    private void validateRegistration(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists");
        }
    }

    private User buildUser(RegisterRequest request, Role role) {
        return User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }
}
