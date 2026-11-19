package com.placementmanagement.api.student.dto;

public final class StudentFeatureDtos {
    private StudentFeatureDtos() {}

    public record DirectoryStudentResponse(Long studentId, String fullName, String enrollmentNumber,
            String academicProgram, String departmentName, String profilePhotoUrl) {}

    public record ComplaintRequest(String subject, String message) {}

    public record OtpRequest(String otp) {}

    public record ChangePasswordRequest(String newPassword, String confirmPassword) {}
}
