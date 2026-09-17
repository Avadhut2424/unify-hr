package com.unifyhr.auth.controller;

import com.unifyhr.auth.dto.AuthResponse;
import com.unifyhr.auth.dto.LoginRequest;
import com.unifyhr.auth.dto.RegisterRequest;
import com.unifyhr.auth.dto.ValidateResponse;
import com.unifyhr.auth.entity.Role;
import com.unifyhr.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_Success_Returns201() {
        RegisterRequest req = new RegisterRequest("newuser", "password123", Role.HR);
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(AuthResponse.registered("newuser", Role.HR));

        ResponseEntity<?> response = authController.register(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void register_Exception_Returns400() {
        RegisterRequest req = new RegisterRequest("existing", "password123", Role.HR);
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        ResponseEntity<?> response = authController.register(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void login_Success_Returns200() {
        LoginRequest req = new LoginRequest("testuser", "password123");
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(AuthResponse.success("jwtToken", "testuser", Role.EMPLOYEE));

        ResponseEntity<?> response = authController.login(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void login_Exception_Returns401() {
        LoginRequest req = new LoginRequest("testuser", "wrong");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid username or password"));

        ResponseEntity<?> response = authController.login(req);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void validate_ValidToken_Returns200() {
        when(authService.validate("validToken"))
                .thenReturn(ValidateResponse.success("testuser", Role.ADMIN));

        ResponseEntity<ValidateResponse> response = authController.validate(null, "validToken");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isValid());
    }

    @Test
    void validate_AuthHeaderPreferred_Returns200() {
        when(authService.validate("Bearer someToken"))
                .thenReturn(ValidateResponse.success("testuser", Role.HR));

        ResponseEntity<ValidateResponse> response = authController.validate("Bearer someToken", "ignoreMe");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isValid());
    }

    @Test
    void validate_InvalidToken_Returns401() {
        when(authService.validate("badToken"))
                .thenReturn(ValidateResponse.invalid("Expired token"));

        ResponseEntity<ValidateResponse> response = authController.validate(null, "badToken");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isValid());
    }
}
