package com.unifyhr.attendance.service;

import com.unifyhr.attendance.dto.AttendanceResponse;
import com.unifyhr.attendance.dto.MarkAttendanceRequest;
import com.unifyhr.attendance.entity.Attendance;
import com.unifyhr.attendance.entity.AttendanceStatus;
import com.unifyhr.attendance.repository.AttendanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private Attendance attendance;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        attendance = new Attendance(1L, today, AttendanceStatus.PRESENT, "admin");
        attendance.setId(10L);
    }

    @Test
    void markAttendance_NewRecord_Success() {
        MarkAttendanceRequest req = new MarkAttendanceRequest(1L, today, AttendanceStatus.PRESENT);
        when(attendanceRepository.findByEmployeeIdAndDate(1L, today)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);

        AttendanceResponse res = attendanceService.markAttendance(req, "admin");

        assertNotNull(res);
        assertEquals(1L, res.getEmployeeId());
        assertEquals(AttendanceStatus.PRESENT, res.getStatus());
        assertEquals("admin", res.getMarkedBy());
    }

    @Test
    void markAttendance_UpdateExisting_Success() {
        MarkAttendanceRequest req = new MarkAttendanceRequest(1L, today, AttendanceStatus.ABSENT);
        when(attendanceRepository.findByEmployeeIdAndDate(1L, today)).thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);

        AttendanceResponse res = attendanceService.markAttendance(req, "hr_user");

        assertNotNull(res);
        assertEquals("hr_user", attendance.getMarkedBy());
        assertEquals(AttendanceStatus.ABSENT, attendance.getStatus());
    }

    @Test
    void getAttendanceByEmployee_Success() {
        when(attendanceRepository.findByEmployeeId(1L)).thenReturn(List.of(attendance));

        List<AttendanceResponse> list = attendanceService.getAttendanceByEmployee(1L);

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getEmployeeId());
    }

    @Test
    void getAllAttendance_Success() {
        when(attendanceRepository.findAll()).thenReturn(List.of(attendance));

        List<AttendanceResponse> list = attendanceService.getAllAttendance();

        assertEquals(1, list.size());
    }

    @Test
    void hasRecordForDate_ChecksRepo() {
        when(attendanceRepository.existsByEmployeeIdAndDate(1L, today)).thenReturn(true);
        assertTrue(attendanceService.hasRecordForDate(1L, today));

        when(attendanceRepository.existsByEmployeeIdAndDate(2L, today)).thenReturn(false);
        assertFalse(attendanceService.hasRecordForDate(2L, today));
    }

    @Test
    void markAbsentee_NewRecord_Success() {
        when(attendanceRepository.existsByEmployeeIdAndDate(2L, today)).thenReturn(false);
        Attendance abs = new Attendance(2L, today, AttendanceStatus.ABSENT, "SYSTEM");
        abs.setId(20L);
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(abs);

        AttendanceResponse res = attendanceService.markAbsentee(2L, today);

        assertNotNull(res);
        assertEquals(AttendanceStatus.ABSENT, res.getStatus());
        assertEquals("SYSTEM", res.getMarkedBy());
    }

    @Test
    void markAbsentee_AlreadyExists_ReturnsNull() {
        when(attendanceRepository.existsByEmployeeIdAndDate(1L, today)).thenReturn(true);

        AttendanceResponse res = attendanceService.markAbsentee(1L, today);

        assertNull(res);
        verify(attendanceRepository, never()).save(any());
    }
}
