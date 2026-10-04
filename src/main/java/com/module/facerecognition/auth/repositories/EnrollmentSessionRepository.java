package com.module.facerecognition.auth.repositories;

import com.module.facerecognition.entities.EnrollmentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnrollmentSessionRepository
        extends JpaRepository<EnrollmentSession, String> {

    Optional<EnrollmentSession>
    findByIdAndUserId(
            String sessionId,
            Long userId
    );
}