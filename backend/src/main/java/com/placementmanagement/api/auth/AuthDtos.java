package com.placementmanagement.api.auth;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            String fullName,
            String enrollmentNumber,
            String universityEmail,
            String password) {}

    public record LoginRequest(String enrollmentNumber, String password) {}

    public record AuthResponse(
            String token,
            String userId,
            String fullName,
            String enrollmentNumber,
            String role) {}
}

