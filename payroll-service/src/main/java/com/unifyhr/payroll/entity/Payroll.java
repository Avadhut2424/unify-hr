package com.unifyhr.payroll.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payrolls")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "base_salary", nullable = false)
    private Double baseSalary;

    @Column(nullable = false)
    private Double deductions;

    @Column(name = "net_pay", nullable = false)
    private Double netPay;

    @Column(name = "pay_period", nullable = false, length = 50)
    private String payPeriod;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Payroll() {
    }

    public Payroll(Long employeeId, Double baseSalary, Double deductions, Double netPay, String payPeriod) {
        this.employeeId = employeeId;
        this.baseSalary = baseSalary;
        this.deductions = deductions;
        this.netPay = netPay;
        this.payPeriod = payPeriod;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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
