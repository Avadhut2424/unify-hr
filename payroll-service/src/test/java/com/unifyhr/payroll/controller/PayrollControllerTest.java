package com.unifyhr.payroll.controller;

import com.unifyhr.payroll.dto.PayrollResponse;
import com.unifyhr.payroll.service.PayrollService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollControllerTest {

    @Mock
    private PayrollService payrollService;

    @InjectMocks
    private PayrollController payrollController;

    private final PayrollResponse sampleResponse = new PayrollResponse(1L, 1L, 50000.0, 1000.0, 49000.0, "2026-09", LocalDateTime.now());

    @Test
    void generatePayroll_Success_Returns201() {
        when(payrollService.generatePayroll(eq(1L), eq("2026-09"), eq("tok123"))).thenReturn(sampleResponse);

        ResponseEntity<?> response = payrollController.generatePayroll(1L, "2026-09", "Bearer tok123");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
    }

    @Test
    void generatePayroll_Exception_Returns400() {
        when(payrollService.generatePayroll(any(), any(), any())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> response = payrollController.generatePayroll(1L, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getPayrollByEmployee_Returns200() {
        when(payrollService.getPayrollByEmployee(1L)).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<PayrollResponse>> response = payrollController.getPayrollByEmployee(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
}
