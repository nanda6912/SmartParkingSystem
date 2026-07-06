package com.smartparking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing a login or authentication status response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private boolean success;
    private String username;
    private String fullName;
    private String role;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String username, String fullName, String role) {
        this.success = success;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
