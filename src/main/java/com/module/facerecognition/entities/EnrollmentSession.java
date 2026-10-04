package com.module.facerecognition.entities;


import com.module.facerecognition.auth.entities.AppUser;
import com.module.facerecognition.auth.enums.EnrollmentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "enrollment_session")
public class EnrollmentSession {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public EnrollmentSession() {
    }

    public EnrollmentSession(
            AppUser user,
            LocalDateTime expiresAt
    ) {
        this.id =
                UUID.randomUUID().toString();

        this.user = user;

        this.status =
                EnrollmentStatus.ACTIVE;

        this.expiresAt =
                expiresAt;

        this.createdAt =
                LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setStatus(
            EnrollmentStatus status
    ) {
        this.status = status;
    }
}