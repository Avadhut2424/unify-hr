package com.unifyhr.attendance.dto;

import com.unifyhr.attendance.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class MarkAttendanceRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    private LocalDate date;

    @NotNull(message = "Attendance status is required")
    private AttendanceStatus status;

    public MarkAttendanceRequest() {
    }

    public MarkAttendanceRequest(Long employeeId, LocalDate date, AttendanceStatus status) {
        this.employeeId = employeeId;
        this.date = date != null ? date : LocalDate.now();
        this.status = status;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getDate() {
        return date != null ? date : LocalDate.now();
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
}
