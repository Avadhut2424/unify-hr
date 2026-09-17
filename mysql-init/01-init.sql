-- Initialize databases for each microservice
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS employee_db;
CREATE DATABASE IF NOT EXISTS attendance_db;
CREATE DATABASE IF NOT EXISTS payroll_db;

FLUSH PRIVILEGES;
