package com.unifyhr.attendance.service;

import com.unifyhr.attendance.dto.AttendanceResponse;
import com.unifyhr.attendance.dto.MarkAttendanceRequest;
import com.unifyhr.attendance.entity.Attendance;
import com.unifyhr.attendance.entity.AttendanceStatus;
import com.unifyhr.attendance.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional
    public AttendanceResponse markAttendance(MarkAttendanceRequest request, String markedBy) {
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();

        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndDate(request.getEmployeeId(), date);
        Attendance attendance;
        if (existing.isPresent()) {
            attendance = existing.get();
            attendance.setStatus(request.getStatus());
            attendance.setMarkedBy(markedBy);
        } else {
            attendance = new Attendance(request.getEmployeeId(), date, request.getStatus(), markedBy);
        }

        Attendance saved = attendanceRepository.save(attendance);
        return AttendanceResponse.fromEntity(saved);
    }

    public List<AttendanceResponse> getAttendanceByEmployee(Long employeeId) {
        return attendanceRepository.findByEmployeeId(employeeId).stream()
                .map(AttendanceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AttendanceResponse> getAllAttendance() {
        return attendanceRepository.findAll().stream()
                .map(AttendanceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public boolean hasRecordForDate(Long employeeId, LocalDate date) {
        return attendanceRepository.existsByEmployeeIdAndDate(employeeId, date);
    }

    @Transactional
    public AttendanceResponse markAbsentee(Long employeeId, LocalDate date) {
        if (attendanceRepository.existsByEmployeeIdAndDate(employeeId, date)) {
            return null; // Already has a record
        }
        Attendance attendance = new Attendance(employeeId, date, AttendanceStatus.ABSENT, "SYSTEM");
        Attendance saved = attendanceRepository.save(attendance);
        return AttendanceResponse.fromEntity(saved);
    }
}
