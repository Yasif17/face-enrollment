package com.module.facerecognition.auth.security;


import com.module.facerecognition.auth.entities.AppUser;
import com.module.facerecognition.auth.repositories.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
public class CurrentUserService {

    private final AppUserRepository userRepository;

    public CurrentUserService(
            AppUserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    public AppUser getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        )
                );
    }
}