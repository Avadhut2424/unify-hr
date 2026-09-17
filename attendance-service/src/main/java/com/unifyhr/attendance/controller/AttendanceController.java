package com.unifyhr.attendance.controller;

import com.unifyhr.attendance.dto.AttendanceResponse;
import com.unifyhr.attendance.dto.MarkAttendanceRequest;
import com.unifyhr.attendance.scheduler.AbsenteeScheduler;
import com.unifyhr.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AbsenteeScheduler absenteeScheduler;

    public AttendanceController(AttendanceService attendanceService, AbsenteeScheduler absenteeScheduler) {
        this.attendanceService = attendanceService;
        this.absenteeScheduler = absenteeScheduler;
    }

    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@Valid @RequestBody MarkAttendanceRequest request,
                                           Authentication authentication) {
        try {
            String markedBy = (authentication != null && authentication.getName() != null)
                    ? authentication.getName() : "HR";
            AttendanceResponse response = attendanceService.markAttendance(request, markedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByEmployee(@PathVariable Long employeeId) {
        List<AttendanceResponse> records = attendanceService.getAttendanceByEmployee(employeeId);
        return ResponseEntity.ok(records);
    }

    @GetMapping
    public ResponseEntity<List<AttendanceResponse>> getAllAttendance() {
        List<AttendanceResponse> records = attendanceService.getAllAttendance();
        return ResponseEntity.ok(records);
    }

    @PostMapping("/trigger-absentee-job")
    public ResponseEntity<?> triggerAbsenteeJob(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;

        // Kicks off job asynchronously without blocking HTTP response
        CompletableFuture<Integer> future = absenteeScheduler.processAbsenteesAsync(token);

        return ResponseEntity.ok(Map.of(
                "status", "Absentee job initiated asynchronously via ExecutorService",
                "message", "Processing employees in background thread pool"
        ));
    }
}
