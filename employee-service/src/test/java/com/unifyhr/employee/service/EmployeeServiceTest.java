package com.unifyhr.employee.service;

import com.unifyhr.employee.dto.EmployeeDTO;
import com.unifyhr.employee.entity.Employee;
import com.unifyhr.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;
    private EmployeeDTO employeeDTO;

    @BeforeEach
    void setUp() {
        employee = new Employee("Jane Doe", "jane@unifyhr.com", "Engineering", "Developer", 75000.0, "ACTIVE");
        employee.setId(1L);

        employeeDTO = new EmployeeDTO(1L, "Jane Doe", "jane@unifyhr.com", "Engineering", "Developer", 75000.0, "ACTIVE");
    }

    @Test
    void createEmployee_Success() {
        when(employeeRepository.existsByEmail("jane@unifyhr.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeDTO created = employeeService.createEmployee(employeeDTO);

        assertNotNull(created);
        assertEquals("Jane Doe", created.getName());
        assertEquals("jane@unifyhr.com", created.getEmail());
    }

    @Test
    void createEmployee_EmailExists_Throws() {
        when(employeeRepository.existsByEmail("jane@unifyhr.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> employeeService.createEmployee(employeeDTO));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void getAllEmployees_Success() {
        when(employeeRepository.findAll()).thenReturn(List.of(employee));

        List<EmployeeDTO> list = employeeService.getAllEmployees();

        assertEquals(1, list.size());
        assertEquals("Jane Doe", list.get(0).getName());
    }

    @Test
    void getEmployeeById_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeDTO found = employeeService.getEmployeeById(1L);

        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void getEmployeeById_NotFound_Throws() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> employeeService.getEmployeeById(99L));
    }

    @Test
    void updateEmployee_Success() {
        EmployeeDTO updateReq = new EmployeeDTO(1L, "Jane Smith", "jane@unifyhr.com", "Product", "PM", 80000.0, "ACTIVE");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeDTO updated = employeeService.updateEmployee(1L, updateReq);

        assertNotNull(updated);
        assertEquals("Jane Smith", employee.getName());
        assertEquals("Product", employee.getDepartment());
    }

    @Test
    void updateEmployee_EmailChangeCollision_Throws() {
        EmployeeDTO updateReq = new EmployeeDTO(1L, "Jane Doe", "collision@unifyhr.com", "Engineering", "Dev", 75000.0, "ACTIVE");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmail("collision@unifyhr.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> employeeService.updateEmployee(1L, updateReq));
    }

    @Test
    void updateEmployee_NotFound_Throws() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> employeeService.updateEmployee(99L, employeeDTO));
    }

    @Test
    void deleteEmployee_Success() {
        when(employeeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(1L);

        employeeService.deleteEmployee(1L);

        verify(employeeRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteEmployee_NotFound_Throws() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> employeeService.deleteEmployee(99L));
        verify(employeeRepository, never()).deleteById(any());
    }
}
