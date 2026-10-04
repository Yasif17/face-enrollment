package com.module.facerecognition.entities;


import com.module.facerecognition.enums.AttendanceStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * The person whose attendance is being recorded.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "person_id",
            nullable = false
    )
    private Person person;

    /*
     * PRESENT / ABSENT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    /*
     * Exact time when attendance was marked.
     */
    @Column(
            name = "attendance_time",
            nullable = false
    )
    private LocalDateTime attendanceTime;

    public Attendance() {
    }

    public Attendance(
            Person person,
            AttendanceStatus status,
            LocalDateTime attendanceTime) {

        this.person = person;
        this.status = status;
        this.attendanceTime = attendanceTime;
    }

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public LocalDateTime getAttendanceTime() {
        return attendanceTime;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public void setAttendanceTime(
            LocalDateTime attendanceTime) {

        this.attendanceTime = attendanceTime;
    }
}