package com.placementmanagement.api.placement.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.placementmanagement.api.auth.dto.AuthDtos.DepartmentInfo;
import com.placementmanagement.api.placement.entity.*;

public final class PlacementDtos {
    private PlacementDtos() {}
    public record CompanyRequest(String name, String description, String website, String hrContactName, String hrContactEmail, String hrContactPhone) {}
    public record CompanyResponse(Long id, String name, String description, String website, String hrContactName, String hrContactEmail, String hrContactPhone) {}
    public record DriveRequest(Long companyId, Long departmentId, String title, String jobDescription, String jobType,
            String location, String packageSalary, Instant applicationDeadline, BigDecimal minimumCgpa,
            Short graduationYear, Integer maximumActiveBacklogs, List<Long> eligibleProgramIds) {}
    public record DriveResponse(Long id, CompanyResponse company, Long departmentId, String title, String jobDescription,
            String jobType, String location, String packageSalary, Instant applicationDeadline, DriveStatus status,
            BigDecimal minimumCgpa, Short graduationYear, Integer maximumActiveBacklogs, List<String> eligibleProgramCodes) {}
    public record ApplicationResponse(Long id, Long driveId, Long studentId, Instant appliedAt, ApplicationStatus status) {}
    public record StaffApplicationResponse(Long id, Long studentId, String studentName, String universityEmail,
            String enrollmentNumber, Long departmentId, String departmentCode, String departmentName,
            String academicProgram, Long driveId, String driveTitle, String companyName,
            Instant appliedAt, ApplicationStatus status, BigDecimal studentCgpa,
            Short studentGraduationYear) {}
    public record StaffApplicationPageResponse(List<StaffApplicationResponse> items, int page, int size,
            long totalElements, int totalPages) {}
    public record StaffStudentResponse(Long id, String fullName, String universityEmail, String enrollmentNumber,
            Long departmentId, String departmentCode, String departmentName, String academicProgram,
            Short graduationYear, BigDecimal cgpa, long applicationCount) {}
    public record StaffStudentPageResponse(List<StaffStudentResponse> items, int page, int size,
            long totalElements, int totalPages) {}
    public record DashboardSummaryResponse(long companies, long drives, long publishedDrives,
            long applications, long selectedApplications, long students, long studentsWithApplications) {}
    public record CoordinatorRequest(String fullName, String universityEmail, Long departmentId, String initialPassword) {}
    public record CoordinatorResponse(Long id, String fullName, String universityEmail,
            DepartmentInfo department, boolean active) {}
    public record RoundRequest(String name, Integer order, Boolean finalRound) {}
    public record RoundResponse(Long id, Long driveId, Integer order, String name, boolean finalRound,
            boolean resultsPublished, long totalCandidates, long passed, long failed, long pending) {}
    public record CandidateResponse(Long applicationId, Long studentId, String studentName,
            String enrollmentNumber, String academicProgram, RoundResultStatus result,
            ApplicationStatus applicationStatus) {}
    public record RoundResultRequest(RoundResultStatus result) {}
    public record FinalOutcomeRequest(ApplicationStatus outcome) {}
    public record RoundResultResponse(Long roundId, Long applicationId, RoundResultStatus result,
            Instant recordedAt) {}
    public record StudentRoundProgress(Long roundId, Integer order, String name,
            boolean resultsPublished, String status) {}
    public record StudentApplicationProgress(Long applicationId, Long driveId, String companyName,
            String driveTitle, Instant appliedAt, ApplicationStatus applicationStatus,
            String finalOutcome, List<StudentRoundProgress> rounds) {}

    public interface StaffApplicationProjection {
        Long getApplicationId();
        Long getStudentId();
        String getStudentName();
        String getUniversityEmail();
        String getEnrollmentNumber();
        Long getDepartmentId();
        String getDepartmentCode();
        String getDepartmentName();
        String getAcademicProgram();
        Long getDriveId();
        String getDriveTitle();
        String getCompanyName();
        Instant getAppliedAt();
        ApplicationStatus getStatus();
        BigDecimal getStudentCgpa();
        Short getStudentGraduationYear();
    }

    public interface StaffStudentProjection {
        Long getStudentId();
        String getFullName();
        String getUniversityEmail();
        String getEnrollmentNumber();
        Long getDepartmentId();
        String getDepartmentCode();
        String getDepartmentName();
        String getAcademicProgram();
        Short getGraduationYear();
        BigDecimal getCgpa();
        long getApplicationCount();
    }
}
