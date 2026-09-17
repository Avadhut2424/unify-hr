package com.unifyhr.auth.dto;

import com.unifyhr.auth.entity.Role;

public class AuthResponse {

    private String token;
    private String username;
    private Role role;
    private String message;

    public AuthResponse() {
    }

    public AuthResponse(String token, String username, Role role, String message) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.message = message;
    }

    public static AuthResponse success(String token, String username, Role role) {
        return new AuthResponse(token, username, role, "Authentication successful");
    }

    public static AuthResponse registered(String username, Role role) {
        return new AuthResponse(null, username, role, "User registered successfully");
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
