package com.module.facerecognition.auth.repositories;

import com.module.facerecognition.auth.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @Query(value = "SELECT * FROM app_user u WHERE u.email ILIKE :email", nativeQuery = true)
    Optional<AppUser> findByEmail(@Param("email") String email);

    boolean existsByEmail(String email);
}