package com.gulfhire.auth.service;

import com.gulfhire.auth.dto.AuthResponse;
import com.gulfhire.auth.dto.LoginRequest;
import com.gulfhire.auth.dto.RegisterRequest;
import com.gulfhire.common.constants.Role;
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
        return AuthResponse.builder()
                .token(token)
                .role(user.getRole())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }
}
