package com.placementmanagement.api.auth.dto;

public final class MeDtos {
    private MeDtos() {}

    public record MeResponse(
            Long userId,
            String fullName,
            String universityEmail,
            String enrollmentNumber,
            String role,
            boolean active,
            DepartmentResponse department) {}

    public record DepartmentResponse(Long id, String code, String name) {}
}
