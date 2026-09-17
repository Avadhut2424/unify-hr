package com.unifyhr.employee.controller;

import com.unifyhr.employee.dto.EmployeeDTO;
import com.unifyhr.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private final EmployeeDTO sampleDTO = new EmployeeDTO(1L, "Alice", "alice@test.com", "IT", "Dev", 70000.0, "ACTIVE");

    @Test
    void createEmployee_Success_Returns201() {
        when(employeeService.createEmployee(any(EmployeeDTO.class))).thenReturn(sampleDTO);

        ResponseEntity<?> response = employeeController.createEmployee(sampleDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(sampleDTO, response.getBody());
    }

    @Test
    void createEmployee_Exception_Returns400() {
        when(employeeService.createEmployee(any(EmployeeDTO.class)))
                .thenThrow(new IllegalArgumentException("Duplicate email"));

        ResponseEntity<?> response = employeeController.createEmployee(sampleDTO);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getAllEmployees_Returns200() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(sampleDTO));

        ResponseEntity<List<EmployeeDTO>> response = employeeController.getAllEmployees();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getEmployeeById_Success_Returns200() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleDTO);

        ResponseEntity<?> response = employeeController.getEmployeeById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getEmployeeById_NotFound_Returns404() {
        when(employeeService.getEmployeeById(99L)).thenThrow(new IllegalArgumentException("Not found"));

        ResponseEntity<?> response = employeeController.getEmployeeById(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateEmployee_Success_Returns200() {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeDTO.class))).thenReturn(sampleDTO);

        ResponseEntity<?> response = employeeController.updateEmployee(1L, sampleDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateEmployee_BadRequest_Returns400() {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeDTO.class)))
                .thenThrow(new IllegalArgumentException("Error"));

        ResponseEntity<?> response = employeeController.updateEmployee(1L, sampleDTO);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deleteEmployee_Success_Returns200() {
        doNothing().when(employeeService).deleteEmployee(1L);

        ResponseEntity<?> response = employeeController.deleteEmployee(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteEmployee_NotFound_Returns404() {
        doThrow(new IllegalArgumentException("Not found")).when(employeeService).deleteEmployee(99L);

        ResponseEntity<?> response = employeeController.deleteEmployee(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
