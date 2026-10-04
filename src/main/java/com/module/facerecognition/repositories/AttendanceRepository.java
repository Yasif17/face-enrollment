package com.module.facerecognition.repositories;


import com.module.facerecognition.entities.Attendance;
import com.module.facerecognition.entities.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByPersonAndAttendanceTimeBetween(
            Person person,
            LocalDateTime start,
            LocalDateTime end
    );
}