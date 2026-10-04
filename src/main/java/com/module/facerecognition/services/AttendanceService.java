package com.module.facerecognition.services;


import com.module.facerecognition.entities.Attendance;
import com.module.facerecognition.entities.Person;
import com.module.facerecognition.enums.AttendanceStatus;
import com.module.facerecognition.repositories.AttendanceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository) {

        this.attendanceRepository =
                attendanceRepository;
    }

    public Attendance markPresent(
            Person person) {

        LocalDate today =
                LocalDate.now();

        LocalDateTime startOfDay =
                today.atStartOfDay();

        LocalDateTime endOfDay =
                today.plusDays(1)
                        .atStartOfDay();

        /*
         * Prevent duplicate attendance
         * for the same person on the same day.
         */
        var existingAttendance =
                attendanceRepository
                        .findByPersonAndAttendanceTimeBetween(
                                person,
                                startOfDay,
                                endOfDay
                        );

        if (existingAttendance.isPresent()) {

            return existingAttendance.get();
        }

        /*
         * First successful verification
         * of the day.
         */
        Attendance attendance =
                new Attendance(
                        person,
                        AttendanceStatus.PRESENT,
                        LocalDateTime.now()
                );

        return attendanceRepository.save(
                attendance
        );
    }
}