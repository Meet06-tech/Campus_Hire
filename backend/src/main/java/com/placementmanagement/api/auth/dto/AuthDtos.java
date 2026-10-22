package com.placementmanagement.api.auth.dto;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            String fullName,
            String enrollmentNumber,
            String universityEmail,
            String password) {}

    /**
     * enrollmentNumber is retained for the existing student client contract.
     * Staff clients may send identifier instead, using the provisioned university email.
     */
    public record LoginRequest(String enrollmentNumber, String password, String identifier) {
        public LoginRequest(String enrollmentNumber, String password) {
            this(enrollmentNumber, password, null);
        }
    }

    public record AuthResponse(
            String token,
            String userId,
            String fullName,
            String enrollmentNumber,
            String role,
            DepartmentInfo department) {
        public AuthResponse(String token, String userId, String fullName,
                String enrollmentNumber, String role) {
            this(token, userId, fullName, enrollmentNumber, role, null);
        }
    }

    public record DepartmentInfo(Long id, String code, String name) {}
}

