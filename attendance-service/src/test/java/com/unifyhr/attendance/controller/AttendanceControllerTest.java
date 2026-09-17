package com.unifyhr.attendance.controller;

import com.unifyhr.attendance.dto.AttendanceResponse;
import com.unifyhr.attendance.dto.MarkAttendanceRequest;
import com.unifyhr.attendance.entity.AttendanceStatus;
import com.unifyhr.attendance.scheduler.AbsenteeScheduler;
import com.unifyhr.attendance.service.AttendanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private AbsenteeScheduler absenteeScheduler;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AttendanceController attendanceController;

    private final AttendanceResponse sampleResponse = new AttendanceResponse(1L, 1L, LocalDate.now(), AttendanceStatus.PRESENT, "admin", LocalDateTime.now());

    @Test
    void markAttendance_Success_Returns201() {
        MarkAttendanceRequest req = new MarkAttendanceRequest(1L, LocalDate.now(), AttendanceStatus.PRESENT);
        when(authentication.getName()).thenReturn("admin");
        when(attendanceService.markAttendance(any(MarkAttendanceRequest.class), eq("admin"))).thenReturn(sampleResponse);

        ResponseEntity<?> response = attendanceController.markAttendance(req, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
    }

    @Test
    void markAttendance_Exception_Returns400() {
        MarkAttendanceRequest req = new MarkAttendanceRequest(1L, LocalDate.now(), AttendanceStatus.PRESENT);
        when(attendanceService.markAttendance(any(), any())).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = attendanceController.markAttendance(req, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getAttendanceByEmployee_Returns200() {
        when(attendanceService.getAttendanceByEmployee(1L)).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<AttendanceResponse>> response = attendanceController.getAttendanceByEmployee(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllAttendance_Returns200() {
        when(attendanceService.getAllAttendance()).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<AttendanceResponse>> response = attendanceController.getAllAttendance();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void triggerAbsenteeJob_Returns200() {
        when(absenteeScheduler.processAbsenteesAsync("token123")).thenReturn(CompletableFuture.completedFuture(2));

        ResponseEntity<?> response = attendanceController.triggerAbsenteeJob("Bearer token123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
