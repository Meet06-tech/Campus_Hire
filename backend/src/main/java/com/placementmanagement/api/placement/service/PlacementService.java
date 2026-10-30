package com.placementmanagement.api.placement.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import com.placementmanagement.api.auth.entity.*;
import com.placementmanagement.api.auth.repository.DepartmentRepository;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.placement.dto.PlacementDtos.*;
import com.placementmanagement.api.placement.entity.*;
import com.placementmanagement.api.placement.repository.*;

@Service
public class PlacementService {
    private final CompanyRepository companies;
    private final PlacementProgramRepository programs;
    private final PlacementDriveRepository drives;
    private final PlacementApplicationRepository applications;
    private final DepartmentRepository departments;
    private final AcademicYearRepository academicYears;
    private final StudentRepository students;
    private final PlacementAuthorizationService authorization;

    public PlacementService(CompanyRepository companies, PlacementProgramRepository programs, PlacementDriveRepository drives,
            PlacementApplicationRepository applications, DepartmentRepository departments, AcademicYearRepository academicYears, StudentRepository students,
            PlacementAuthorizationService authorization) {
        this.companies = companies; this.programs = programs; this.drives = drives; this.applications = applications;
        this.departments = departments; this.academicYears = academicYears; this.students = students; this.authorization = authorization;
    }

    @Transactional
    public CompanyResponse createCompany(Authentication auth, CompanyRequest request) {
        authorization.staff(auth);
        if (request == null) throw bad("Company is required");
        String name = required(request.name(), "Company name");
        return company(companies.save(new Company(name, request.description(), request.website(), request.hrContactName(),
                request.hrContactEmail(), request.hrContactPhone())));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> listCompanies(Authentication auth) {
        authorization.staff(auth);
        return companies.findAll().stream().map(this::company).toList();
    }

    @Transactional
    public DriveResponse createDrive(Authentication auth, DriveRequest request) {
        UserAccount user = authorization.staff(auth);
        validateDrive(request);
        Company company = companies.findById(request.companyId()).orElseThrow(() -> notFound("Company"));
        AcademicYear academicYear = academicYears.findByStatus("ACTIVE")
                .orElseThrow(() -> bad("No active academic year is configured"));
        Department department;
        if (user.getRole() == UserRole.DTPO) {
            if (user.getDepartment() == null) throw bad("DTPO has no assigned department");
            if (request.departmentId() != null && !request.departmentId().equals(user.getDepartment().getId()))
                throw forbidden("DTPO cannot create a drive for another department");
            department = user.getDepartment();
        } else {
            department = request.departmentId() == null ? null : departments.findById(request.departmentId()).orElseThrow(() -> notFound("Department"));
            if (department != null && !department.isActive()) throw bad("Department is inactive");
        }
        Set<PlacementProgram> selected = resolvePrograms(request.eligibleProgramIds());
        PlacementDrive drive = drives.save(new PlacementDrive(academicYear, company, department, user, request.title(), request.jobDescription(),
                request.jobType(), request.location(), request.packageSalary(), Instant.now(), request.applicationDeadline(), request.minimumCgpa(),
                request.graduationYear(), request.maximumActiveBacklogs(), selected));
        return drive(drive);
    }

    @Transactional(readOnly = true)
    public List<DriveResponse> listStaffDrives(Authentication auth) {
        UserAccount user = authorization.staff(auth);
        List<PlacementDrive> result = user.getRole() == UserRole.DTPO
                ? drives.findAllByDepartment_IdOrderByApplicationDeadlineAsc(requireDepartment(user).getId())
                : drives.findAllByOrderByApplicationDeadlineAsc();
        return result.stream().map(this::drive).toList();
    }

    @Transactional
    public DriveResponse cancelDrive(Authentication auth, Long driveId) {
        UserAccount user = authorization.staff(auth);
        PlacementDrive drive = drives.findById(driveId).orElseThrow(() -> notFound("Placement drive"));
        if (user.getRole() == UserRole.DTPO) {
            Department department = requireDepartment(user);
            if (drive.getDepartment() == null || !department.getId().equals(drive.getDepartment().getId()))
                throw forbidden("DTPO cannot cancel a drive outside its department");
        }
        if (drive.getStatus() != DriveStatus.DRAFT && drive.getStatus() != DriveStatus.PUBLISHED)
            throw bad("Only draft or published drives can be cancelled");
        drive.setStatus(DriveStatus.CANCELLED);
        return drive(drive);
    }

    @Transactional
    public DriveResponse publishDrive(Authentication auth, Long driveId) {
        UserAccount user = authorization.staff(auth);
        PlacementDrive drive = drives.findById(driveId).orElseThrow(() -> notFound("Placement drive"));
        authorizePublish(user, drive);
        if (drive.getStatus() != DriveStatus.DRAFT)
            throw bad("Only draft placement drives can be published");
        validatePublishableDrive(drive);
        drive.setStatus(DriveStatus.PUBLISHED);
        return drive(drive);
    }

    @Transactional(readOnly = true)
    public List<DriveResponse> listStudentDrives(Authentication auth) {
        Student student = authorization.student(auth);
        return drives.findVisiblePublished(DriveStatus.PUBLISHED, student.getDepartment().getId()).stream()
                .filter(d -> d.getStatus() == DriveStatus.PUBLISHED)
                .filter(d -> eligible(student, d)).map(this::drive).toList();
    }

    @Transactional
    public ApplicationResponse apply(Authentication auth, Long driveId) {
        Student student = authorization.student(auth);
        if (applications.existsByStudent_IdAndOfferStatus(student.getId(), OfferStatus.ACCEPTED))
            throw bad("You have already accepted a placement offer. You cannot apply for another drive.");
        PlacementDrive drive = drives.findById(driveId).orElseThrow(() -> notFound("Placement drive"));
        if (drive.getStatus() != DriveStatus.PUBLISHED) throw bad("Applications are closed for this drive");
        if (drive.getApplicationDeadline().isBefore(Instant.now())) throw bad("Application deadline has passed");
        if (drive.getDepartment() != null && !drive.getDepartment().getId().equals(student.getDepartment().getId())) throw forbidden("Drive is outside your department");
        if (!eligible(student, drive)) throw forbidden("Student does not meet the drive eligibility rules");
        if (applications.existsByDrive_IdAndStudent_Id(driveId, student.getId())) throw conflict("You have already applied to this placement drive");
        return application(applications.save(new PlacementApplication(drive, student)));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> myApplications(Authentication auth) {
        Student student = authorization.student(auth);
        return applications.findAllByStudent_IdOrderByAppliedAtDesc(student.getId()).stream().map(this::application).toList();
    }

    @Transactional
    public ApplicationResponse decideOffer(Authentication auth, Long applicationId, OfferDecisionRequest request) {
        Student student = authorization.student(auth);
        if (request == null || request.decision() == null) throw bad("Offer decision is required");
        PlacementApplication application = applications.findById(applicationId)
                .orElseThrow(() -> notFound("Application"));
        if (!application.getStudent().getId().equals(student.getId()))
            throw forbidden("Application does not belong to the current student");
        if (application.getStatus() != ApplicationStatus.SELECTED)
            throw bad("Only selected applications can receive an offer decision");
        if (application.getOfferStatus() != null)
            throw conflict("This offer has already been decided");
        application.decideOffer(request.decision());
        applications.save(application);
        return application(application);
    }

    @Transactional(readOnly = true)
    public StaffApplicationPageResponse listStaffApplications(Authentication auth, int page, int size,
            String search, ApplicationStatus status, Long companyId,
            BigDecimal minCgpa, BigDecimal maxCgpa) {
        UserAccount user = authorization.staff(auth);
        Pageable pageable = PageRequest.of(page, size);
        Long departmentId = scopedDepartmentId(user);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        if (minCgpa != null && maxCgpa != null && minCgpa.compareTo(maxCgpa) > 0)
            throw bad("Minimum CGPA cannot exceed maximum CGPA");
        Page<StaffApplicationProjection> result = applications.findStaffApplications(departmentId, status, companyId,
                minCgpa, maxCgpa, normalizedSearch, pageable);
        return new StaffApplicationPageResponse(result.getContent().stream().map(this::staffApplication).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public StaffStudentPageResponse listStaffStudents(Authentication auth, int page, int size, String search) {
        UserAccount user = authorization.staff(auth);
        Pageable pageable = PageRequest.of(page, size);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Page<StaffStudentProjection> result = students.findStaffStudents(scopedDepartmentId(user), normalizedSearch, pageable);
        return new StaffStudentPageResponse(result.getContent().stream().map(this::staffStudent).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse dashboardSummary(Authentication auth) {
        UserAccount user = authorization.staff(auth);
        Long departmentId = scopedDepartmentId(user);
        long drivesCount = departmentId == null ? drives.count() : drives.countByDepartment_Id(departmentId);
        long publishedCount = departmentId == null ? drives.countByStatus(DriveStatus.PUBLISHED)
                : drives.countByDepartment_IdAndStatus(departmentId, DriveStatus.PUBLISHED);
        long applicationCount = departmentId == null ? applications.count() : applications.countByDrive_Department_Id(departmentId);
        long selectedCount = departmentId == null ? applications.countByStatus(ApplicationStatus.SELECTED)
                : applications.countByDrive_Department_IdAndStatus(departmentId, ApplicationStatus.SELECTED);
        long studentCount = departmentId == null ? students.count() : students.countByDepartment_Id(departmentId);
        long studentsWithApplications = applications.countDistinctStudentsByDriveDepartment(departmentId);
        return new DashboardSummaryResponse(companies.count(), drivesCount, publishedCount, applicationCount, selectedCount,
                studentCount, studentsWithApplications);
    }

    private boolean eligible(Student student, PlacementDrive drive) {
        if (drive.getMinimumCgpa() != null && (student.getCgpa() == null || student.getCgpa().compareTo(drive.getMinimumCgpa()) < 0)) return false;
        if (drive.getGraduationYear() != null && !drive.getGraduationYear().equals(student.getGraduationYear())) return false;
        if (!drive.getPrograms().isEmpty()) {
            String value = student.getAcademicProgram();
            if (value == null) return false;
            boolean match = drive.getPrograms().stream().anyMatch(p -> p.getCode().equals(value.trim()));
            if (!match) return false;
        }
        // The current students table has no active-backlog field. Fail closed when this rule is configured.
        return drive.getMaximumActiveBacklogs() == null;
    }

    private Set<PlacementProgram> resolvePrograms(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new LinkedHashSet<>();
        Set<Long> unique = new LinkedHashSet<>(ids);
        if (unique.size() != ids.size()) throw bad("Duplicate eligible program IDs are not allowed");
        List<PlacementProgram> found = programs.findAllByIdIn(unique);
        if (found.size() != unique.size() || found.stream().anyMatch(p -> !p.isActive())) throw bad("Every selected program must exist and be active");
        return new LinkedHashSet<>(found);
    }

    private void authorizePublish(UserAccount user, PlacementDrive drive) {
        if (user.getRole() == UserRole.DTPO) {
            Department department = requireDepartment(user);
            if (drive.getDepartment() == null || !department.getId().equals(drive.getDepartment().getId()))
                throw forbidden("DTPO cannot publish a drive outside its department");
        }
    }

    private void validatePublishableDrive(PlacementDrive drive) {
        if (drive.getCompany() == null) throw bad("Company is required");
        if (drive.getAcademicYear() == null) throw bad("Academic year is required");
        AcademicYear activeYear = academicYears.findByStatus("ACTIVE")
                .orElseThrow(() -> bad("No active academic year is configured"));
        if (activeYear.getId() == null || !activeYear.getId().equals(drive.getAcademicYear().getId()))
            throw bad("Drive academic year is not active");
        required(drive.getTitle(), "Title");
        required(drive.getJobDescription(), "Job description");
        required(drive.getJobType(), "Job type");
        required(drive.getLocation(), "Location");
        required(drive.getPackageSalary(), "Package/salary");
        if (!Set.of("ONSITE", "HYBRID", "REMOTE").contains(drive.getJobType().trim().toUpperCase(Locale.ROOT)))
            throw bad("Job type must be ONSITE, HYBRID, or REMOTE");
        if (drive.getApplicationDeadline() == null || drive.getApplicationDeadline().isBefore(Instant.now()))
            throw bad("Application deadline must be in the future");
        if (drive.getMinimumCgpa() != null && (drive.getMinimumCgpa().compareTo(BigDecimal.ZERO) < 0
                || drive.getMinimumCgpa().compareTo(BigDecimal.TEN) > 0))
            throw bad("Minimum CGPA must be between 0 and 10");
        if (drive.getMaximumActiveBacklogs() != null && drive.getMaximumActiveBacklogs() < 0)
            throw bad("Maximum active backlogs cannot be negative");
        if (drive.getPrograms() == null || drive.getPrograms().stream().anyMatch(program -> program == null || !program.isActive()))
            throw bad("Every selected program must exist and be active");
    }
    private Long scopedDepartmentId(UserAccount user) {
        if (user.getRole() != UserRole.DTPO) return null;
        return requireDepartment(user).getId();
    }
    private void validateDrive(DriveRequest r) {
        if (r == null) throw bad("Placement drive is required");
        required(r.title(), "Title"); required(r.jobDescription(), "Job description"); required(r.jobType(), "Job type");
        required(r.location(), "Location"); required(r.packageSalary(), "Package/salary");
        if (!Set.of("ONSITE", "HYBRID", "REMOTE").contains(r.jobType().trim().toUpperCase(Locale.ROOT)))
            throw bad("Job type must be ONSITE, HYBRID, or REMOTE");
        if (r.companyId() == null || r.applicationDeadline() == null) throw bad("Company and deadline are required");
        if (r.applicationDeadline().isBefore(Instant.now())) throw bad("Application deadline must be in the future");
        if (r.minimumCgpa() != null && (r.minimumCgpa().compareTo(BigDecimal.ZERO) < 0 || r.minimumCgpa().compareTo(BigDecimal.TEN) > 0)) throw bad("Minimum CGPA must be between 0 and 10");
        if (r.maximumActiveBacklogs() != null && r.maximumActiveBacklogs() < 0) throw bad("Maximum active backlogs cannot be negative");
    }
    private Department requireDepartment(UserAccount user) { if (user.getDepartment() == null) throw bad("Staff department is not configured"); return user.getDepartment(); }
    private CompanyResponse company(Company c) { return new CompanyResponse(c.getId(), c.getName(), c.getDescription(), c.getWebsite(), c.getHrContactName(), c.getHrContactEmail(), c.getHrContactPhone()); }
    private DriveResponse drive(PlacementDrive d) { return new DriveResponse(d.getId(), company(d.getCompany()), d.getDepartment() == null ? null : d.getDepartment().getId(), d.getTitle(), d.getJobDescription(), d.getJobType(), d.getLocation(), d.getPackageSalary(), d.getApplicationDeadline(), d.getStatus(), d.getMinimumCgpa(), d.getGraduationYear(), d.getMaximumActiveBacklogs(), d.getPrograms().stream().map(PlacementProgram::getCode).toList()); }
    private ApplicationResponse application(PlacementApplication a) { return new ApplicationResponse(a.getId(), a.getDrive().getId(), a.getStudent().getId(), a.getAppliedAt(), a.getStatus(), a.getOfferStatus(), a.getOfferDecidedAt()); }
    private StaffApplicationResponse staffApplication(StaffApplicationProjection p) {
        return new StaffApplicationResponse(p.getApplicationId(), p.getStudentId(), p.getStudentName(), p.getUniversityEmail(),
                p.getEnrollmentNumber(), p.getDepartmentId(), p.getDepartmentCode(), p.getDepartmentName(), p.getAcademicProgram(),
                p.getDriveId(), p.getDriveTitle(), p.getCompanyName(), p.getAppliedAt(), p.getStatus(),
                p.getStudentCgpa(), p.getStudentGraduationYear());
    }
    private StaffStudentResponse staffStudent(StaffStudentProjection p) {
        return new StaffStudentResponse(p.getStudentId(), p.getFullName(), p.getUniversityEmail(), p.getEnrollmentNumber(),
                p.getDepartmentId(), p.getDepartmentCode(), p.getDepartmentName(), p.getAcademicProgram(), p.getGraduationYear(),
                p.getCgpa(), p.getApplicationCount());
    }
    private String required(String value, String field) { if (value == null || value.isBlank()) throw bad(field + " is required"); return value.trim(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException forbidden(String message) { return new ResponseStatusException(HttpStatus.FORBIDDEN, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private ResponseStatusException notFound(String type) { return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found"); }
}
