package com.placementmanagement.api.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.DepartmentRepository;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.placement.dto.PlacementDtos.ApplicationResponse;
import com.placementmanagement.api.placement.dto.PlacementDtos.StaffApplicationPageResponse;
import com.placementmanagement.api.placement.dto.PlacementDtos.StaffStudentPageResponse;
import com.placementmanagement.api.placement.dto.PlacementDtos.CompanyRequest;
import com.placementmanagement.api.placement.dto.PlacementDtos.DriveRequest;
import com.placementmanagement.api.placement.dto.PlacementDtos.DriveResponse;
import com.placementmanagement.api.placement.dto.PlacementDtos.OfferDecisionRequest;
import com.placementmanagement.api.placement.entity.AcademicYear;
import com.placementmanagement.api.placement.entity.ApplicationStatus;
import com.placementmanagement.api.placement.entity.Company;
import com.placementmanagement.api.placement.entity.DriveStatus;
import com.placementmanagement.api.placement.entity.PlacementApplication;
import com.placementmanagement.api.placement.entity.PlacementDrive;
import com.placementmanagement.api.placement.entity.PlacementProgram;
import com.placementmanagement.api.placement.entity.OfferStatus;
import com.placementmanagement.api.placement.repository.AcademicYearRepository;
import com.placementmanagement.api.placement.repository.CompanyRepository;
import com.placementmanagement.api.placement.repository.PlacementApplicationRepository;
import com.placementmanagement.api.placement.repository.PlacementDriveRepository;
import com.placementmanagement.api.placement.repository.PlacementProgramRepository;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;
import com.placementmanagement.api.placement.service.PlacementService;

@ExtendWith(MockitoExtension.class)
class PlacementServiceTest {
    @Mock CompanyRepository companies;
    @Mock PlacementProgramRepository programs;
    @Mock PlacementDriveRepository drives;
    @Mock PlacementApplicationRepository applications;
    @Mock DepartmentRepository departments;
    @Mock AcademicYearRepository academicYears;
    @Mock StudentRepository students;
    @Mock PlacementAuthorizationService authorization;
    @Mock Authentication authentication;
    private PlacementService service;

    @BeforeEach
    void setUp() {
        service = new PlacementService(companies, programs, drives, applications, departments,
                academicYears, students, authorization);
    }

    @Test
    void staffCanCreateAndListCompanies() {
        UserAccount staff = mock(UserAccount.class);
        when(authorization.staff(authentication)).thenReturn(staff);
        when(companies.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.createCompany(authentication,
                new CompanyRequest("Acme", "Profile", "https://acme.example", "HR", "hr@acme.example", "123"));
        var listed = service.listCompanies(authentication);

        assertEquals("Acme", created.name());
        assertEquals(List.of(), listed);
        verify(companies).save(any(Company.class));
        verify(companies).findAll();
    }

    @Test
    void dtpoDriveIsForcedToItsAssignedDepartment() {
        UserAccount dtpo = mock(UserAccount.class);
        Department department = department(2L);
        Company company = mock(Company.class);
        AcademicYear year = mock(AcademicYear.class);
        when(authorization.staff(authentication)).thenReturn(dtpo);
        when(dtpo.getRole()).thenReturn(UserRole.DTPO);
        when(dtpo.getDepartment()).thenReturn(department);
        when(companies.findById(5L)).thenReturn(Optional.of(company));
        when(academicYears.findByStatus("ACTIVE")).thenReturn(Optional.of(year));
        when(drives.save(any(PlacementDrive.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriveResponse response = service.createDrive(authentication, driveRequest(5L, null, null));

        assertEquals(2L, response.departmentId());
        verify(departments, never()).findById(anyLong());
    }

    @Test
    void dtpoCannotCreateForAnotherDepartment() {
        UserAccount dtpo = mock(UserAccount.class);
        Department department = department(2L);
        Company company = mock(Company.class);
        AcademicYear year = mock(AcademicYear.class);
        when(authorization.staff(authentication)).thenReturn(dtpo);
        when(dtpo.getRole()).thenReturn(UserRole.DTPO);
        when(dtpo.getDepartment()).thenReturn(department);
        when(companies.findById(5L)).thenReturn(Optional.of(company));
        when(academicYears.findByStatus("ACTIVE")).thenReturn(Optional.of(year));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createDrive(authentication, driveRequest(5L, 3L, null)));

        assertEquals(403, error.getStatusCode().value());
        verify(drives, never()).save(any());
    }

    @Test
    void rejectsDriveWithPastApplicationDeadline() {
        UserAccount utpo = mock(UserAccount.class);
        when(authorization.staff(authentication)).thenReturn(utpo);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createDrive(authentication, new DriveRequest(5L, null, "Backend Engineer",
                        "Build APIs", "REMOTE", "Remote", "8 LPA", Instant.now().minusSeconds(60),
                        null, null, null, List.of())));

        assertEquals(400, error.getStatusCode().value());
        assertEquals("Application deadline must be in the future", error.getReason());
        verifyNoInteractions(companies, academicYears, drives);
    }

    @Test
    void dtpoListsOnlyAssignedDepartmentDrivesWhileUtpoListsAll() {
        UserAccount dtpo = mock(UserAccount.class);
        UserAccount utpo = mock(UserAccount.class);
        Department department = department(2L);
        when(dtpo.getRole()).thenReturn(UserRole.DTPO);
        when(dtpo.getDepartment()).thenReturn(department);
        when(utpo.getRole()).thenReturn(UserRole.UTPO);
        when(authorization.staff(authentication)).thenReturn(dtpo, utpo);
        when(drives.findAllByDepartment_IdOrderByApplicationDeadlineAsc(2L)).thenReturn(List.of());
        when(drives.findAllByOrderByApplicationDeadlineAsc()).thenReturn(List.of());

        service.listStaffDrives(authentication);
        service.listStaffDrives(authentication);

        verify(drives).findAllByDepartment_IdOrderByApplicationDeadlineAsc(2L);
        verify(drives).findAllByOrderByApplicationDeadlineAsc();
    }

    @Test
    void authorizedDtpoCanPublishOwnDraftDrive() {
        UserAccount dtpo = mock(UserAccount.class);
        Department department = department(2L);
        PlacementDrive drive = publishableDrive(DriveStatus.DRAFT, department, 1L);
        when(authorization.staff(authentication)).thenReturn(dtpo);
        when(dtpo.getRole()).thenReturn(UserRole.DTPO);
        when(dtpo.getDepartment()).thenReturn(department);
        when(drives.findById(9L)).thenReturn(Optional.of(drive));

        service.publishDrive(authentication, 9L);

        verify(drive).setStatus(DriveStatus.PUBLISHED);
        verify(academicYears).findByStatus("ACTIVE");
    }

    @Test
    void publishingNonDraftDriveIsRejected() {
        UserAccount utpo = mock(UserAccount.class);
        PlacementDrive drive = mock(PlacementDrive.class);
        when(authorization.staff(authentication)).thenReturn(utpo);
        when(utpo.getRole()).thenReturn(UserRole.UTPO);
        when(drives.findById(9L)).thenReturn(Optional.of(drive));
        when(drive.getStatus()).thenReturn(DriveStatus.PUBLISHED);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.publishDrive(authentication, 9L));

        assertEquals(400, error.getStatusCode().value());
        verify(drive, never()).setStatus(any());
        verifyNoInteractions(academicYears);
    }

    @Test
    void dtpoCannotPublishAnotherDepartmentsDrive() {
        UserAccount dtpo = mock(UserAccount.class);
        Department ownDepartment = department(2L);
        Department otherDepartment = department(3L);
        PlacementDrive drive = mock(PlacementDrive.class);
        when(authorization.staff(authentication)).thenReturn(dtpo);
        when(dtpo.getRole()).thenReturn(UserRole.DTPO);
        when(dtpo.getDepartment()).thenReturn(ownDepartment);
        when(drives.findById(9L)).thenReturn(Optional.of(drive));
        when(drive.getDepartment()).thenReturn(otherDepartment);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.publishDrive(authentication, 9L));

        assertEquals(403, error.getStatusCode().value());
        verify(drive, never()).setStatus(any());
        verifyNoInteractions(academicYears);
    }

    @Test
    void authorizedDtpoCanCancelOwnPublishedDrive() {
        UserAccount dtpo = mock(UserAccount.class);
        Department department = department(2L);
        Company company = mock(Company.class);
        PlacementDrive drive = mock(PlacementDrive.class);
        when(authorization.staff(authentication)).thenReturn(dtpo);
        when(dtpo.getRole()).thenReturn(UserRole.DTPO);
        when(dtpo.getDepartment()).thenReturn(department);
        when(drives.findById(9L)).thenReturn(Optional.of(drive));
        when(drive.getCompany()).thenReturn(company);
        when(drive.getDepartment()).thenReturn(department);
        when(drive.getPrograms()).thenReturn(Set.of());
        when(drive.getStatus()).thenReturn(DriveStatus.PUBLISHED);

        service.cancelDrive(authentication, 9L);

        verify(drive).setStatus(DriveStatus.CANCELLED);
    }

    @Test
    void dtpoCannotCancelAnotherDepartmentsDrive() {
        UserAccount dtpo = mock(UserAccount.class);
        Department ownDepartment = department(2L);
        Department otherDepartment = department(3L);
        PlacementDrive drive = mock(PlacementDrive.class);
        when(authorization.staff(authentication)).thenReturn(dtpo);
        when(dtpo.getRole()).thenReturn(UserRole.DTPO);
        when(dtpo.getDepartment()).thenReturn(ownDepartment);
        when(drives.findById(9L)).thenReturn(Optional.of(drive));
        when(drive.getDepartment()).thenReturn(otherDepartment);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.cancelDrive(authentication, 9L));

        assertEquals(403, error.getStatusCode().value());
        verify(drive, never()).setStatus(any());
    }

    @Test
    void unauthenticatedPublishIsRejectedBeforeDriveLookup() {
        when(authorization.staff(authentication)).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication is required"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.publishDrive(authentication, 9L));

        assertEquals(401, error.getStatusCode().value());
        verifyNoInteractions(drives, academicYears);
    }

    @Test
    void studentSeesPublishedDepartmentAndUniversityWideDrivesOnlyWhenEligible() {
        Student student = mock(Student.class);
        Department department = department(2L);
        PlacementDrive eligible = drive(DriveStatus.PUBLISHED, null, 7, 2029, "MCA");
        PlacementDrive cancelled = mock(PlacementDrive.class);
        PlacementDrive ineligible = mock(PlacementDrive.class);
        when(ineligible.getStatus()).thenReturn(DriveStatus.PUBLISHED);
        when(ineligible.getMinimumCgpa()).thenReturn(new java.math.BigDecimal("9"));
        when(cancelled.getStatus()).thenReturn(DriveStatus.CANCELLED);
        when(authorization.student(authentication)).thenReturn(student);
        when(student.getDepartment()).thenReturn(department);
        when(student.getCgpa()).thenReturn(new java.math.BigDecimal("8.0"));
        when(student.getGraduationYear()).thenReturn((short) 2029);
        when(student.getAcademicProgram()).thenReturn("MCA");
        when(drives.findVisiblePublished(DriveStatus.PUBLISHED, 2L)).thenReturn(List.of(eligible, cancelled, ineligible));

        List<DriveResponse> result = service.listStudentDrives(authentication);

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).minimumCgpa().intValue());
        verify(drives).findVisiblePublished(DriveStatus.PUBLISHED, 2L);
    }

    @Test
    void studentCannotApplyToAnotherDepartmentDrive() {
        Student student = mock(Student.class);
        Department studentDepartment = department(2L);
        Department otherDepartment = department(3L);
        PlacementDrive drive = mock(PlacementDrive.class);
        when(authorization.student(authentication)).thenReturn(student);
        when(student.getDepartment()).thenReturn(studentDepartment);
        when(drives.findById(9L)).thenReturn(Optional.of(drive));
        when(drive.getStatus()).thenReturn(DriveStatus.PUBLISHED);
        when(drive.getApplicationDeadline()).thenReturn(Instant.now().plusSeconds(3600));
        when(drive.getDepartment()).thenReturn(otherDepartment);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.apply(authentication, 9L));

        assertEquals(403, error.getStatusCode().value());
        verify(applications, never()).save(any());
    }

    @Test
    void studentCannotApplyToUnpublishedDrive() {
        Student student = mock(Student.class);
        PlacementDrive drive = mock(PlacementDrive.class);
        when(authorization.student(authentication)).thenReturn(student);
        when(drives.findById(9L)).thenReturn(Optional.of(drive));
        when(drive.getStatus()).thenReturn(DriveStatus.DRAFT);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.apply(authentication, 9L));

        assertEquals(400, error.getStatusCode().value());
        verify(applications, never()).save(any());
    }

    @Test
    void studentCannotApplyToCancelledDrive() {
        Student student = mock(Student.class);
        PlacementDrive drive = mock(PlacementDrive.class);
        when(authorization.student(authentication)).thenReturn(student);
        when(drives.findById(9L)).thenReturn(Optional.of(drive));
        when(drive.getStatus()).thenReturn(DriveStatus.CANCELLED);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.apply(authentication, 9L));

        assertEquals(400, error.getStatusCode().value());
        verify(applications, never()).save(any());
    }

    @Test
    void studentWithAcceptedOfferCannotApplyToAnotherDrive() {
        Student student = mock(Student.class);
        when(authorization.student(authentication)).thenReturn(student);
        when(student.getId()).thenReturn(20L);
        when(applications.existsByStudent_IdAndOfferStatus(20L, OfferStatus.ACCEPTED)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.apply(authentication, 9L));

        assertEquals(400, error.getStatusCode().value());
        assertEquals("You have already accepted a placement offer. You cannot apply for another drive.", error.getReason());
        verifyNoInteractions(drives);
    }

    @Test
    void selectedStudentCanDecideOfferOnlyOnce() {
        Student student = mock(Student.class);
        PlacementApplication application = mock(PlacementApplication.class);
        PlacementDrive selectedDrive = mock(PlacementDrive.class);
        when(authorization.student(authentication)).thenReturn(student);
        when(student.getId()).thenReturn(20L);
        when(application.getStudent()).thenReturn(student);
        when(application.getStatus()).thenReturn(ApplicationStatus.SELECTED);
        when(application.getOfferStatus()).thenReturn(null);
        when(application.getDrive()).thenReturn(selectedDrive);
        when(selectedDrive.getId()).thenReturn(9L);
        when(application.getId()).thenReturn(30L);
        when(application.getAppliedAt()).thenReturn(Instant.now());
        when(applications.findById(30L)).thenReturn(Optional.of(application));
        when(applications.save(application)).thenReturn(application);

        service.decideOffer(authentication, 30L, new OfferDecisionRequest(OfferStatus.ACCEPTED));

        verify(application).decideOffer(OfferStatus.ACCEPTED);
        verify(applications).save(application);
    }

    @Test
    void repeatedOfferDecisionIsRejected() {
        Student student = mock(Student.class);
        PlacementApplication application = mock(PlacementApplication.class);
        when(authorization.student(authentication)).thenReturn(student);
        when(student.getId()).thenReturn(20L);
        when(application.getStudent()).thenReturn(student);
        when(application.getStatus()).thenReturn(ApplicationStatus.SELECTED);
        when(application.getOfferStatus()).thenReturn(OfferStatus.DECLINED);
        when(applications.findById(30L)).thenReturn(Optional.of(application));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.decideOffer(authentication, 30L, new OfferDecisionRequest(OfferStatus.ACCEPTED)));

        assertEquals(409, error.getStatusCode().value());
        verify(applications, never()).save(any());
    }

    @Test
    void ineligibleStudentCannotApplyToPublishedDrive() {
        Student student = mock(Student.class);
        Department department = department(2L);
        PlacementDrive drive = mock(PlacementDrive.class);
        when(authorization.student(authentication)).thenReturn(student);
        when(student.getDepartment()).thenReturn(department);
        when(student.getCgpa()).thenReturn(new java.math.BigDecimal("6.9"));
        when(drives.findById(9L)).thenReturn(Optional.of(drive));
        when(drive.getStatus()).thenReturn(DriveStatus.PUBLISHED);
        when(drive.getApplicationDeadline()).thenReturn(Instant.now().plusSeconds(3600));
        when(drive.getDepartment()).thenReturn(department);
        when(drive.getMinimumCgpa()).thenReturn(new java.math.BigDecimal("7.0"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.apply(authentication, 9L));

        assertEquals(403, error.getStatusCode().value());
        verify(applications, never()).save(any());
    }

    @Test
    void unauthenticatedStudentCannotApply() {
        when(authorization.student(authentication)).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication is required"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.apply(authentication, 9L));

        assertEquals(401, error.getStatusCode().value());
        verifyNoInteractions(drives, applications);
    }

    @Test
    void dtpoApplicationListingUsesAuthenticatedDepartmentScopeAndFilters() {
        UserAccount dtpo = mock(UserAccount.class);
        Department department = department(2L);
        when(authorization.staff(authentication)).thenReturn(dtpo);
        when(dtpo.getRole()).thenReturn(UserRole.DTPO);
        when(dtpo.getDepartment()).thenReturn(department);
        when(applications.findStaffApplications(eq(2L), eq(ApplicationStatus.SHORTLISTED), isNull(), isNull(), isNull(), eq("mca"), any(Pageable.class)))
                .thenReturn(Page.empty());

        StaffApplicationPageResponse response = service.listStaffApplications(authentication, 0, 25, "mca", ApplicationStatus.SHORTLISTED, null, null, null);

        assertEquals(0, response.totalElements());
        verify(applications).findStaffApplications(eq(2L), eq(ApplicationStatus.SHORTLISTED), isNull(), isNull(), isNull(), eq("mca"), any(Pageable.class));
    }

    @Test
    void applicationListingPassesCompanyAndCgpaFilters() {
        UserAccount utpo = mock(UserAccount.class);
        when(authorization.staff(authentication)).thenReturn(utpo);
        when(utpo.getRole()).thenReturn(UserRole.UTPO);
        when(applications.findStaffApplications(isNull(), isNull(), eq(4L), eq(new java.math.BigDecimal("7.00")),
                eq(new java.math.BigDecimal("9.00")), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        service.listStaffApplications(authentication, 0, 25, null, null, 4L,
                new java.math.BigDecimal("7.00"), new java.math.BigDecimal("9.00"));

        verify(applications).findStaffApplications(isNull(), isNull(), eq(4L), eq(new java.math.BigDecimal("7.00")),
                eq(new java.math.BigDecimal("9.00")), isNull(), any(Pageable.class));
    }

    @Test
    void utpoApplicationListingUsesUniversityScopeAndAllowsEmptyResults() {
        UserAccount utpo = mock(UserAccount.class);
        when(authorization.staff(authentication)).thenReturn(utpo);
        when(utpo.getRole()).thenReturn(UserRole.UTPO);
        when(applications.findStaffApplications(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        StaffApplicationPageResponse response = service.listStaffApplications(authentication, 0, 25, "", null, null, null, null);

        assertEquals(0, response.items().size());
        verify(applications).findStaffApplications(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void dtpoStudentListingUsesAuthenticatedDepartmentScopeWithoutSearch() {
        UserAccount dtpo = mock(UserAccount.class);
        Department department = department(2L);
        when(authorization.staff(authentication)).thenReturn(dtpo);
        when(dtpo.getRole()).thenReturn(UserRole.DTPO);
        when(dtpo.getDepartment()).thenReturn(department);
        when(students.findStaffStudents(eq(2L), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        StaffStudentPageResponse response = service.listStaffStudents(authentication, 0, 25, null);

        assertEquals(0, response.totalElements());
        verify(students).findStaffStudents(eq(2L), isNull(), any(Pageable.class));
    }

    @Test
    void utpoStudentListingUsesUniversityScopeWithSearch() {
        UserAccount utpo = mock(UserAccount.class);
        when(authorization.staff(authentication)).thenReturn(utpo);
        when(utpo.getRole()).thenReturn(UserRole.UTPO);
        when(students.findStaffStudents(isNull(), eq("mca"), any(Pageable.class))).thenReturn(Page.empty());

        StaffStudentPageResponse response = service.listStaffStudents(authentication, 0, 25, "  mca ");

        assertEquals(0, response.items().size());
        verify(students).findStaffStudents(isNull(), eq("mca"), any(Pageable.class));
    }

    @Test
    void studentCannotUseStaffApplicationListing() {
        when(authorization.staff(authentication)).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Staff access is required"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.listStaffApplications(authentication, 0, 25, null, null, null, null, null));

        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(applications);
    }

    @Test
    void studentCanApplyOnceAndDuplicateApplicationIsRejected() {
        Student student = mock(Student.class);
        PlacementDrive drive = mock(PlacementDrive.class);
        PlacementApplication saved = mock(PlacementApplication.class);
        when(authorization.student(authentication)).thenReturn(student);
        when(student.getId()).thenReturn(20L);
        when(drives.findById(9L)).thenReturn(Optional.of(drive));
        when(drive.getStatus()).thenReturn(DriveStatus.PUBLISHED);
        when(drive.getApplicationDeadline()).thenReturn(Instant.now().plusSeconds(3600));
        when(drive.getDepartment()).thenReturn(null);
        when(drive.getPrograms()).thenReturn(Set.of());
        when(drive.getMaximumActiveBacklogs()).thenReturn(null);
        when(applications.existsByDrive_IdAndStudent_Id(9L, 20L)).thenReturn(false, true);
        when(applications.save(any(PlacementApplication.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(30L);
        when(saved.getDrive()).thenReturn(drive);
        when(saved.getStudent()).thenReturn(student);
        when(saved.getAppliedAt()).thenReturn(Instant.now());
        when(saved.getStatus()).thenReturn(ApplicationStatus.APPLIED);

        ApplicationResponse response = service.apply(authentication, 9L);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.apply(authentication, 9L));

        assertEquals(30L, response.id());
        assertEquals(409, error.getStatusCode().value());
        verify(applications).save(any(PlacementApplication.class));
    }

    @Test
    void studentCannotUseStaffCompanyOperations() {
        when(authorization.staff(authentication)).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Staff access is required"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.listCompanies(authentication));

        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(companies);
    }

    private DriveRequest driveRequest(Long companyId, Long departmentId, java.util.List<Long> programs) {
        return new DriveRequest(companyId, departmentId, "Backend Engineer", "Build APIs", "REMOTE", "Remote",
                "100000", Instant.now().plusSeconds(3600), null, null, null, programs);
    }

    private Department department(Long id) {
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(id);
        return department;
    }

    private PlacementDrive drive(DriveStatus status, Department department, int minimumCgpa,
            int graduationYear, String programCode) {
        PlacementDrive drive = mock(PlacementDrive.class);
        PlacementProgram program = mock(PlacementProgram.class);
        Company company = mock(Company.class);
        when(program.getCode()).thenReturn(programCode);
        when(drive.getStatus()).thenReturn(status);
        when(drive.getDepartment()).thenReturn(department);
        when(drive.getPrograms()).thenReturn(Set.of(program));
        when(drive.getMinimumCgpa()).thenReturn(new java.math.BigDecimal(minimumCgpa));
        when(drive.getGraduationYear()).thenReturn((short) graduationYear);
        when(drive.getMaximumActiveBacklogs()).thenReturn(null);
        when(drive.getCompany()).thenReturn(company);
        when(drive.getApplicationDeadline()).thenReturn(Instant.now().plusSeconds(3600));
        when(drive.getTitle()).thenReturn("Backend Engineer");
        when(drive.getJobDescription()).thenReturn("Build APIs");
        when(drive.getJobType()).thenReturn("REMOTE");
        when(drive.getLocation()).thenReturn("Remote");
        when(drive.getPackageSalary()).thenReturn("100000");
        return drive;
    }

    private PlacementDrive publishableDrive(DriveStatus status, Department department, Long academicYearId) {
        PlacementDrive drive = mock(PlacementDrive.class);
        AcademicYear year = mock(AcademicYear.class);
        Company company = mock(Company.class);
        when(year.getId()).thenReturn(academicYearId);
        when(drive.getStatus()).thenReturn(status);
        when(drive.getDepartment()).thenReturn(department);
        when(drive.getAcademicYear()).thenReturn(year);
        when(drive.getCompany()).thenReturn(company);
        when(drive.getTitle()).thenReturn("Backend Engineer");
        when(drive.getJobDescription()).thenReturn("Build APIs");
        when(drive.getJobType()).thenReturn("REMOTE");
        when(drive.getLocation()).thenReturn("Remote");
        when(drive.getPackageSalary()).thenReturn("100000");
        when(drive.getApplicationDeadline()).thenReturn(Instant.now().plusSeconds(3600));
        when(drive.getMinimumCgpa()).thenReturn(new java.math.BigDecimal("7"));
        when(drive.getGraduationYear()).thenReturn((short) 2029);
        when(drive.getMaximumActiveBacklogs()).thenReturn(null);
        when(academicYears.findByStatus("ACTIVE")).thenReturn(Optional.of(year));
        when(drive.getPrograms()).thenReturn(Set.of());
        return drive;
    }
}
