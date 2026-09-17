package com.unifyhr.payroll.dto;

import com.unifyhr.payroll.entity.Payroll;

import java.time.LocalDateTime;

public class PayrollResponse {

    private Long id;
    private Long employeeId;
    private Double baseSalary;
    private Double deductions;
    private Double netPay;
    private String payPeriod;
    private LocalDateTime createdAt;

    public PayrollResponse() {
    }

    public PayrollResponse(Long id, Long employeeId, Double baseSalary, Double deductions, Double netPay, String payPeriod, LocalDateTime createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.baseSalary = baseSalary;
        this.deductions = deductions;
        this.netPay = netPay;
        this.payPeriod = payPeriod;
        this.createdAt = createdAt;
    }

    public static PayrollResponse fromEntity(Payroll payroll) {
        return new PayrollResponse(
                payroll.getId(),
                payroll.getEmployeeId(),
                payroll.getBaseSalary(),
                payroll.getDeductions(),
                payroll.getNetPay(),
                payroll.getPayPeriod(),
                payroll.getCreatedAt()
        );
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

    public Double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(Double baseSalary) {
        this.baseSalary = baseSalary;
    }

    public Double getDeductions() {
        return deductions;
    }

    public void setDeductions(Double deductions) {
        this.deductions = deductions;
    }

    public Double getNetPay() {
        return netPay;
    }

    public void setNetPay(Double netPay) {
        this.netPay = netPay;
    }

    public String getPayPeriod() {
        return payPeriod;
    }

    public void setPayPeriod(String payPeriod) {
        this.payPeriod = payPeriod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
