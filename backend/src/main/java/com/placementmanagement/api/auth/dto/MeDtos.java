package com.placementmanagement.api.auth.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class MeDtos {
    private MeDtos() {}

    public record MeResponse(
            Long userId,
            String fullName,
            String universityEmail,
            String enrollmentNumber,
            String role,
            boolean active,
            DepartmentResponse department,
            ProfileResponse profile,
            String profilePhotoUrl) {}

    public record ProfileResponse(
            Short admissionYear,
            Short graduationYear,
            LocalDate dateOfBirth,
            String phoneNumber,
            String academicProgram,
            Short currentSemester,
            BigDecimal cgpa,
            String careerProfile,
            List<String> skills,
            Integer programDurationSemesters) {}

    public record ProfileUpdateRequest(
            String fullName,
            Integer admissionYear,
            Integer graduationYear,
            LocalDate dateOfBirth,
            String phoneNumber,
            String academicProgram,
            Integer currentSemester,
            BigDecimal cgpa,
            String careerProfile,
            List<String> skills) {
        public ProfileUpdateRequest(String fullName, Integer admissionYear, Integer graduationYear,
                LocalDate dateOfBirth, String phoneNumber, String academicProgram,
                Integer currentSemester, BigDecimal cgpa, String careerProfile) {
            this(fullName, admissionYear, graduationYear, dateOfBirth, phoneNumber, academicProgram,
                    currentSemester, cgpa, careerProfile, null);
        }
    }

    public record DepartmentResponse(Long id, String code, String name) {}
}
