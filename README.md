# UnifyHR — Microservices-Based Employee & Payroll Management System

[![CI/CD Pipeline](https://github.com/unifyhr/unifyhr/actions/workflows/ci.yml/badge.svg)](https://github.com/unifyhr/unifyhr/actions/workflows/ci.yml)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/unifyhr/unifyhr/actions)
[![Coverage](https://img.shields.io/badge/coverage->=90%25-brightgreen)](https://github.com/unifyhr/unifyhr)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A modular enterprise HR and Payroll management platform built on Spring Boot microservices, Spring Security, JWT authentication, MySQL, and Docker.

---

## Architecture Overview

UnifyHR is divided into 4 independent microservices:

1. **Auth Service (`auth-service/`)**:
   - Handles user registration, authentication, and JWT issuance.
   - Enforces 3-tier Role-Based Access Control (RBAC): `Admin`, `HR`, `Employee`.
2. **Employee Service (`employee-service/`)**:
   - Manages employee profiles, onboarding, departments, and designations.
   - Supports 1,000+ employee records.
3. **Attendance Service (`attendance-service/`)**:
   - Automates attendance tracking and absentee processing.
   - Leverages multithreading (`@Scheduled` + `ExecutorService`) to auto-mark 200+ absentees daily without blocking concurrent HTTP requests.
4. **Payroll Service (`payroll-service/`)**:
   - Calculates monthly salaries, deductions, taxes, and generates payslips based on attendance.

---

## Infrastructure & Deployment

- **Network**: Dedicated bridge network `unifyhr-net` for inter-service communication.
- **Database**: MySQL per-service or shared database schema instances.
- **Containerization**: Docker Compose orchestration for local and staging deployments.
- **CI/CD**: GitHub Actions workflow for automated testing and container builds.

---

## Directory Structure

```text
unifyhr/
├── auth-service/           # Auth Microservice (JWT, Spring Security)
├── employee-service/       # Employee Management Microservice
├── attendance-service/     # Attendance & Multi-threaded Scheduled Task Service
├── payroll-service/        # Payroll Processing Microservice
├── docker-compose.yml      # Root orchestration file
├── .env                    # Environment variables configuration
├── .gitignore              # Git ignore rules for Java, Maven, Docker, and IDEs
└── README.md               # Project documentation
```

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Git
