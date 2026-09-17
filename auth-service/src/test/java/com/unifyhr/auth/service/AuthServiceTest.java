package com.unifyhr.auth.service;

import com.unifyhr.auth.dto.AuthResponse;
import com.unifyhr.auth.dto.LoginRequest;
import com.unifyhr.auth.dto.RegisterRequest;
import com.unifyhr.auth.dto.ValidateResponse;
import com.unifyhr.auth.entity.Role;
import com.unifyhr.auth.entity.User;
import com.unifyhr.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("testuser", "hashedPassword", Role.EMPLOYEE);
        sampleUser.setId(1L);
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("newuser", "password123", Role.HR);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedSecret");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals(Role.HR, response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_DefaultRole_Success() {
        RegisterRequest request = new RegisterRequest("newuser", "password123", null);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedSecret");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(Role.EMPLOYEE, response.getRole());
    }

    @Test
    void register_ExistingUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest("testuser", "password123", Role.EMPLOYEE);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken("testuser", Role.EMPLOYEE)).thenReturn("mockJwtToken");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals(Role.EMPLOYEE, response.getRole());
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest("unknown", "password123");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        LoginRequest request = new LoginRequest("testuser", "wrongpass");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpass", "hashedPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void validate_ValidToken_ReturnsSuccess() {
        String token = "validToken";
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn("testuser");
        when(jwtService.extractRole(token)).thenReturn(Role.EMPLOYEE);

        ValidateResponse response = authService.validate(token);

        assertTrue(response.isValid());
        assertEquals("testuser", response.getUsername());
        assertEquals(Role.EMPLOYEE, response.getRole());
    }

    @Test
    void validate_BearerTokenPrefix_ReturnsSuccess() {
        String rawToken = "myRawToken";
        when(jwtService.isTokenValid(rawToken)).thenReturn(true);
        when(jwtService.extractUsername(rawToken)).thenReturn("testuser");
        when(jwtService.extractRole(rawToken)).thenReturn(Role.ADMIN);

        ValidateResponse response = authService.validate("Bearer " + rawToken);

        assertTrue(response.isValid());
        assertEquals(Role.ADMIN, response.getRole());
    }

    @Test
    void validate_NullOrBlank_ReturnsInvalid() {
        ValidateResponse resNull = authService.validate(null);
        assertFalse(resNull.isValid());

        ValidateResponse resBlank = authService.validate("   ");
        assertFalse(resBlank.isValid());
    }

    @Test
    void validate_ExpiredOrInvalid_ReturnsInvalid() {
        when(jwtService.isTokenValid("badToken")).thenReturn(false);

        ValidateResponse response = authService.validate("badToken");

        assertFalse(response.isValid());
    }
}
