package com.unifyhr.auth.service;

import com.unifyhr.auth.dto.AuthResponse;
import com.unifyhr.auth.dto.LoginRequest;
import com.unifyhr.auth.dto.RegisterRequest;
import com.unifyhr.auth.dto.ValidateResponse;
import com.unifyhr.auth.entity.Role;
import com.unifyhr.auth.entity.User;
import com.unifyhr.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.EMPLOYEE;
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(request.getUsername(), encodedPassword, role);
        userRepository.save(user);

        return AuthResponse.registered(user.getUsername(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return AuthResponse.success(token, user.getUsername(), user.getRole());
    }

    public ValidateResponse validate(String token) {
        if (token == null || token.isBlank()) {
            return ValidateResponse.invalid("Token is required");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        if (!jwtService.isTokenValid(token)) {
            return ValidateResponse.invalid("Invalid or expired token");
        }

        String username = jwtService.extractUsername(token);
        Role role = jwtService.extractRole(token);

        return ValidateResponse.success(username, role);
    }
}
