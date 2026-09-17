package com.unifyhr.payroll.service;

import com.unifyhr.payroll.client.AttendanceServiceClient;
import com.unifyhr.payroll.client.EmployeeServiceClient;
import com.unifyhr.payroll.dto.AttendanceDTO;
import com.unifyhr.payroll.dto.EmployeeDTO;
import com.unifyhr.payroll.dto.PayrollResponse;
import com.unifyhr.payroll.entity.Payroll;
import com.unifyhr.payroll.repository.PayrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private EmployeeServiceClient employeeServiceClient;

    @Mock
    private AttendanceServiceClient attendanceServiceClient;

    @InjectMocks
    private PayrollService payrollService;

    private EmployeeDTO employee;
    private Payroll payroll;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(payrollService, "deductionPerAbsentDay", 1000.0);
        employee = new EmployeeDTO(1L, "Alice", "alice@unifyhr.com", "Dev", "Lead", 60000.0, "ACTIVE");
        payroll = new Payroll(1L, 60000.0, 2000.0, 58000.0, "2026-09");
        payroll.setId(5L);
    }

    @Test
    void generatePayroll_WithAbsentees_CalculatesNetPay() {
        AttendanceDTO a1 = new AttendanceDTO(1L, 1L, LocalDate.now().minusDays(1), "ABSENT", "SYSTEM");
        AttendanceDTO a2 = new AttendanceDTO(2L, 1L, LocalDate.now().minusDays(2), "ABSENT", "SYSTEM");
        AttendanceDTO a3 = new AttendanceDTO(3L, 1L, LocalDate.now().minusDays(3), "PRESENT", "admin");

        when(employeeServiceClient.getEmployeeById(1L, "token")).thenReturn(employee);
        when(attendanceServiceClient.getAttendanceByEmployee(1L, "token")).thenReturn(List.of(a1, a2, a3));
        when(payrollRepository.save(any(Payroll.class))).thenReturn(payroll);

        PayrollResponse res = payrollService.generatePayroll(1L, "2026-09", "token");

        assertNotNull(res);
        assertEquals(1L, res.getEmployeeId());
        assertEquals(60000.0, res.getBaseSalary());
        assertEquals(2000.0, res.getDeductions());
        assertEquals(58000.0, res.getNetPay());
    }

    @Test
    void generatePayroll_NoAbsentees_ZeroDeductions() {
        when(employeeServiceClient.getEmployeeById(1L, null)).thenReturn(employee);
        when(attendanceServiceClient.getAttendanceByEmployee(1L, null)).thenReturn(Collections.emptyList());
        Payroll zeroDed = new Payroll(1L, 60000.0, 0.0, 60000.0, "2026-09");
        when(payrollRepository.save(any(Payroll.class))).thenReturn(zeroDed);

        PayrollResponse res = payrollService.generatePayroll(1L, null, null);

        assertNotNull(res);
        assertEquals(0.0, res.getDeductions());
        assertEquals(60000.0, res.getNetPay());
    }

    @Test
    void generatePayroll_FallbackSalaryWhenEmployeeServiceUnavailable() {
        when(employeeServiceClient.getEmployeeById(2L, "tok")).thenReturn(null);
        when(attendanceServiceClient.getAttendanceByEmployee(2L, "tok")).thenReturn(Collections.emptyList());
        Payroll fallback = new Payroll(2L, 5000.0, 0.0, 5000.0, "2026-09");
        when(payrollRepository.save(any(Payroll.class))).thenReturn(fallback);

        PayrollResponse res = payrollService.generatePayroll(2L, "2026-09", "tok");

        assertNotNull(res);
        assertEquals(5000.0, res.getBaseSalary());
    }

    @Test
    void getPayrollByEmployee_ReturnsList() {
        when(payrollRepository.findByEmployeeId(1L)).thenReturn(List.of(payroll));

        List<PayrollResponse> list = payrollService.getPayrollByEmployee(1L);

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getEmployeeId());
    }
}
