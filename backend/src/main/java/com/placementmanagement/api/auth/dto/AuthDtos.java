package com.placementmanagement.api.auth.dto;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            String fullName,
            String enrollmentNumber,
            String universityEmail,
            String password,
            String confirmPassword) {
        public RegisterRequest(String fullName, String enrollmentNumber, String universityEmail, String password) {
            this(fullName, enrollmentNumber, universityEmail, password, null);
        }
    }

    public record LoginRequest(String identifier, String enrollmentNumber, String password) {
        /** Backward-compatible constructor for existing student callers/tests. */
        public LoginRequest(String enrollmentNumber, String password) {
            this(null, enrollmentNumber, password);
        }
    }

    public record AuthResponse(
            String token,
            String userId,
            String fullName,
            String enrollmentNumber,
            String role,
            DepartmentInfo department) {
        /** Backward-compatible constructor for existing response callers/tests. */
        public AuthResponse(String token, String userId, String fullName, String enrollmentNumber, String role) {
            this(token, userId, fullName, enrollmentNumber, role, null);
        }
    }

    public record DepartmentInfo(Long id, String code, String name) {}
}

