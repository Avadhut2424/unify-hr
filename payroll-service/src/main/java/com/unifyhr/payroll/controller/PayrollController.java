package com.unifyhr.payroll.controller;

import com.unifyhr.payroll.dto.PayrollResponse;
import com.unifyhr.payroll.service.PayrollService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @PostMapping("/generate/{employeeId}")
    public ResponseEntity<?> generatePayroll(
            @PathVariable Long employeeId,
            @RequestParam(value = "payPeriod", required = false) String payPeriod,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            String token = (authHeader != null && authHeader.startsWith("Bearer "))
                    ? authHeader.substring(7) : null;

            PayrollResponse response = payrollService.generatePayroll(employeeId, payPeriod, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<List<PayrollResponse>> getPayrollByEmployee(@PathVariable Long employeeId) {
        List<PayrollResponse> payrolls = payrollService.getPayrollByEmployee(employeeId);
        return ResponseEntity.ok(payrolls);
    }
}
