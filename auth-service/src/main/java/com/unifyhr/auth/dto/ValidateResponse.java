package com.unifyhr.auth.dto;

import com.unifyhr.auth.entity.Role;

public class ValidateResponse {

    private boolean valid;
    private String username;
    private Role role;
    private String message;

    public ValidateResponse() {
    }

    public ValidateResponse(boolean valid, String username, Role role, String message) {
        this.valid = valid;
        this.username = username;
        this.role = role;
        this.message = message;
    }

    public static ValidateResponse success(String username, Role role) {
        return new ValidateResponse(true, username, role, "Token is valid");
    }

    public static ValidateResponse invalid(String message) {
        return new ValidateResponse(false, null, null, message);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
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
