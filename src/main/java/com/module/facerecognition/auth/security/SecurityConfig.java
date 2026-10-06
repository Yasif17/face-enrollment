package com.module.facerecognition.auth.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        // 1. Allow internal JSP view forwards and error dispatches
                        .dispatcherTypeMatchers(
                                DispatcherType.FORWARD,
                                DispatcherType.INCLUDE,
                                DispatcherType.ERROR
                        ).permitAll()

                        // 2. Allow public JSP views & static assets
                        .requestMatchers(
                                "/",
                                "/enroll",
                                "/live",
                                "/live-enroll",
                                "/WEB-INF/views/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                // PWA files
                                "/manifest.json",
                                "/sw.js",
                                "/icons/**",
                                "/api/antispoof/**"
                        ).permitAll()

                        // 3. Allow authentication endpoints (login, register)
                        .requestMatchers("/api/auth/**").permitAll()

                        // 4. Allow public live face attendance endpoints
                        .requestMatchers(
                                "/api/faces/live-frame",
                                "/api/faces/live-verify",
                                "/api/faces/detect",
                                "/api/liveness/**"
                        ).permitAll()

                        // 5. All other API endpoints require JWT authentication
                        .anyRequest().authenticated()
                )

                // IMPORTANT:
                // Read JWT before Spring's normal authentication filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
