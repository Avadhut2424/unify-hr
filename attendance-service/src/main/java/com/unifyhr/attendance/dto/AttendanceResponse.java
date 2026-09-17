package com.unifyhr.attendance.dto;

import com.unifyhr.attendance.entity.Attendance;
import com.unifyhr.attendance.entity.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceResponse {

    private Long id;
    private Long employeeId;
    private LocalDate date;
    private AttendanceStatus status;
    private String markedBy;
    private LocalDateTime createdAt;

    public AttendanceResponse() {
    }

    public AttendanceResponse(Long id, Long employeeId, LocalDate date, AttendanceStatus status, String markedBy, LocalDateTime createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.date = date;
        this.status = status;
        this.markedBy = markedBy;
        this.createdAt = createdAt;
    }

    public static AttendanceResponse fromEntity(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getEmployeeId(),
                attendance.getDate(),
                attendance.getStatus(),
                attendance.getMarkedBy(),
                attendance.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
