package com.module.facerecognition.auth.services;


import com.module.facerecognition.auth.dtos.AuthResponse;
import com.module.facerecognition.auth.dtos.LoginRequest;
import com.module.facerecognition.auth.dtos.RegisterRequest;
import com.module.facerecognition.auth.entities.AppUser;
import com.module.facerecognition.auth.repositories.AppUserRepository;
import com.module.facerecognition.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {

        validateRegisterRequest(request);

        String email =
                request.email()
                        .trim()
                        .toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email already registered"
            );
        }

        String passwordHash =
                passwordEncoder.encode(
                        request.password()
                );

        AppUser user =
                new AppUser(
                        request.name().trim(),
                        email,
                        passwordHash
                );

        user = userRepository.save(user);

        String token =
                jwtService.generateToken(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name()
                );

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public AuthResponse login(LoginRequest request) {


        String email =
                request.email()
                        .trim()
                        .toLowerCase();

        System.out.println("LOGIN EMAIL = [" + email + "]");

        AppUser user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid email or password"
                                )
                        );

        if (!user.isEnabled()) {
            throw new IllegalArgumentException(
                    "Account is disabled"
            );
        }

        System.out.println("LOGIN EMAIL = [" + email + "]");
        System.out.println("PASSWORD LENGTH = " +
                request.password().length());
        System.out.println("HASH = [" +
                user.getPasswordHash() + "]");

        System.out.println(
                "PASSWORD MATCH = " +
                        passwordEncoder.matches(
                                request.password(),
                                user.getPasswordHash()
                        )
        );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        String token =
                jwtService.generateToken(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name()
                );

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail()
        );



    }

    private void validateRegisterRequest(
            RegisterRequest request
    ) {

        if (request.name() == null ||
                request.name().isBlank()) {

            throw new IllegalArgumentException(
                    "Name is required"
            );
        }

        if (request.email() == null ||
                request.email().isBlank()) {

            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        if (request.password() == null ||
                request.password().length() < 8) {

            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters"
            );
        }
    }
}