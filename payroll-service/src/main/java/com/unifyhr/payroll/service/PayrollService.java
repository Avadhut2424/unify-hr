package com.unifyhr.payroll.service;

import com.unifyhr.payroll.client.AttendanceServiceClient;
import com.unifyhr.payroll.client.EmployeeServiceClient;
import com.unifyhr.payroll.dto.AttendanceDTO;
import com.unifyhr.payroll.dto.EmployeeDTO;
import com.unifyhr.payroll.dto.PayrollResponse;
import com.unifyhr.payroll.entity.Payroll;
import com.unifyhr.payroll.repository.PayrollRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollRepository payrollRepository;
    private final EmployeeServiceClient employeeServiceClient;
    private final AttendanceServiceClient attendanceServiceClient;

    @Value("${payroll.deduction-per-absent-day:1000.0}")
    private double deductionPerAbsentDay;

    public PayrollService(PayrollRepository payrollRepository,
                          EmployeeServiceClient employeeServiceClient,
                          AttendanceServiceClient attendanceServiceClient) {
        this.payrollRepository = payrollRepository;
        this.employeeServiceClient = employeeServiceClient;
        this.attendanceServiceClient = attendanceServiceClient;
    }

    /**
     * Inter-Service Calculation Strategy:
     * 1. Fetches employee profile from employee-service via REST to get base salary.
     * 2. Fetches attendance history from attendance-service via REST to count ABSENT days.
     * 3. Applies deduction formula:
     *      deductions = absent_days * deduction_per_absent_day ($1,000.00 / day)
     *      net_pay    = base_salary - deductions
     */
    @Transactional
    public PayrollResponse generatePayroll(Long employeeId, String payPeriod, String token) {
        log.info("Generating payroll for employee ID: {}", employeeId);

        // Step 1: Inter-service call to employee-service for base salary
        EmployeeDTO employee = employeeServiceClient.getEmployeeById(employeeId, token);
        double baseSalary = (employee != null && employee.getSalary() != null)
                ? employee.getSalary()
                : 5000.0; // fallback if employee-service is unavailable

        // Step 2: Inter-service call to attendance-service for attendance history
        List<AttendanceDTO> attendanceList = attendanceServiceClient.getAttendanceByEmployee(employeeId, token);

        long absentDays = attendanceList.stream()
                .filter(a -> a.getStatus() != null && "ABSENT".equalsIgnoreCase(a.getStatus()))
                .count();

        // Step 3: Calculate deductions and net pay
        double deductions = absentDays * deductionPerAbsentDay;
        double netPay = Math.max(0.0, baseSalary - deductions);

        String period = (payPeriod != null && !payPeriod.isBlank())
                ? payPeriod
                : YearMonth.now().toString();

        log.info("Payroll calculation for employee {}: Base={}, AbsentDays={}, Deductions={}, NetPay={}",
                employeeId, baseSalary, absentDays, deductions, netPay);

        Payroll payroll = new Payroll(employeeId, baseSalary, deductions, netPay, period);
        Payroll saved = payrollRepository.save(payroll);

        return PayrollResponse.fromEntity(saved);
    }

    public List<PayrollResponse> getPayrollByEmployee(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId).stream()
                .map(PayrollResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
