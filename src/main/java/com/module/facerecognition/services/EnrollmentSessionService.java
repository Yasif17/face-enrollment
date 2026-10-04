package com.module.facerecognition.services;

import com.module.facerecognition.auth.entities.AppUser;
import com.module.facerecognition.auth.enums.EnrollmentStatus;
import com.module.facerecognition.auth.repositories.EnrollmentSessionRepository;
import com.module.facerecognition.auth.security.CurrentUserService;
import com.module.facerecognition.entities.EnrollmentSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EnrollmentSessionService {

    private final CurrentUserService currentUserService;
    private final EnrollmentSessionRepository repository;

    public EnrollmentSessionService(
            CurrentUserService currentUserService,
            EnrollmentSessionRepository repository
    ) {
        this.currentUserService = currentUserService;
        this.repository = repository;
    }

    @Transactional
    public EnrollmentSession start() {

        AppUser user =
                currentUserService.getCurrentUser();

        EnrollmentSession session =
                new EnrollmentSession(
                        user,
                        LocalDateTime.now()
                                .plusMinutes(2)
                );

        return repository.save(session);
    }

    public EnrollmentSession getActive(
            String sessionId
    ) {

        AppUser user =
                currentUserService.getCurrentUser();

        EnrollmentSession session =
                repository
                        .findByIdAndUserId(
                                sessionId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid enrollment session"
                                )
                        );

        if (session.getStatus()
                != EnrollmentStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "Enrollment session is not active"
            );
        }

        if (session.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            session.setStatus(
                    EnrollmentStatus.EXPIRED
            );

            repository.save(session);

            throw new IllegalArgumentException(
                    "Enrollment session expired"
            );
        }

        return session;
    }

    // =========================================================
    // COMPLETE ENROLLMENT SESSION
    // =========================================================

    @Transactional
    public EnrollmentSession complete(
            String sessionId
    ) {

        EnrollmentSession session =
                getActive(sessionId);

        session.setStatus(
                EnrollmentStatus.COMPLETED
        );

        return repository.save(session);
    }
}