package com.unifyhr.attendance.scheduler;

import com.unifyhr.attendance.client.EmployeeServiceClient;
import com.unifyhr.attendance.dto.EmployeeDTO;
import com.unifyhr.attendance.service.AttendanceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbsenteeSchedulerTest {

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private EmployeeServiceClient employeeServiceClient;

    private ExecutorService executorService;
    private AbsenteeScheduler absenteeScheduler;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        absenteeScheduler = new AbsenteeScheduler(executorService, attendanceService, employeeServiceClient);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    void runScheduledAbsenteeJob_FiresSuccessfully() {
        when(employeeServiceClient.getAllEmployees(null)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> absenteeScheduler.runScheduledAbsenteeJob());
    }

    @Test
    void processAbsenteesAsync_WithAbsentees_MarksSuccessfully() throws Exception {
        EmployeeDTO emp1 = new EmployeeDTO(1L, "Alice", "alice@test.com", "IT", "ACTIVE");
        EmployeeDTO emp2 = new EmployeeDTO(2L, "Bob", "bob@test.com", "HR", "ACTIVE");

        when(employeeServiceClient.getAllEmployees(any())).thenReturn(List.of(emp1, emp2));
        // emp1 does not have record (absent), emp2 has record
        when(attendanceService.hasRecordForDate(eq(1L), any(LocalDate.class))).thenReturn(false);
        when(attendanceService.hasRecordForDate(eq(2L), any(LocalDate.class))).thenReturn(true);

        CompletableFuture<Integer> future = absenteeScheduler.processAbsenteesAsync("dummyToken");
        int markedCount = future.get();

        assertEquals(1, markedCount);
        verify(attendanceService, times(1)).markAbsentee(eq(1L), any(LocalDate.class));
        verify(attendanceService, never()).markAbsentee(eq(2L), any(LocalDate.class));
    }

    @Test
    void processAbsenteesAsync_EmptyEmployeeList_ReturnsZero() throws Exception {
        when(employeeServiceClient.getAllEmployees(any())).thenReturn(Collections.emptyList());

        CompletableFuture<Integer> future = absenteeScheduler.processAbsenteesAsync(null);
        int markedCount = future.get();

        assertEquals(0, markedCount);
        verify(attendanceService, never()).markAbsentee(any(), any());
    }
}
