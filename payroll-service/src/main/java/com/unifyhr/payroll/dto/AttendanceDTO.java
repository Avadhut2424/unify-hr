package com.unifyhr.payroll.dto;

import java.time.LocalDate;

public class AttendanceDTO {
    private Long id;
    private Long employeeId;
    private LocalDate date;
    private String status;
    private String markedBy;

    public AttendanceDTO() {
    }

    public AttendanceDTO(Long id, Long employeeId, LocalDate date, String status, String markedBy) {
        this.id = id;
        this.employeeId = employeeId;
        this.date = date;
        this.status = status;
        this.markedBy = markedBy;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }
}
