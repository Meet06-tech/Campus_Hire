package com.placementmanagement.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.MeDtos.MeResponse;
import com.placementmanagement.api.auth.dto.MeDtos.ProfileUpdateRequest;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.service.CurrentUserService;
import com.placementmanagement.api.placement.entity.PlacementProgram;
import com.placementmanagement.api.placement.repository.PlacementProgramRepository;

class CurrentUserServiceTest {
    @Mock
    private UserAccountRepository users;
    @Mock
    private StudentRepository students;
    @Mock
    private PlacementProgramRepository programs;
    @Mock
    private UserAccount user;
    @Mock
    private Student student;
    @Mock
    private Department department;

    private CurrentUserService service;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CurrentUserService(users, students);
        authentication = new UsernamePasswordAuthenticationToken(
                "42", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(user.isEnabled()).thenReturn(true);
        when(user.getId()).thenReturn(42L);
        when(user.getFullName()).thenReturn("Test Student");
        when(user.getUniversityEmail()).thenReturn("student@charusat.edu.in");
        when(user.getRole()).thenReturn(UserRole.STUDENT);
        when(user.getStudent()).thenReturn(student);
        when(student.getEnrollmentNumber()).thenReturn("25MCA001");
        when(student.getDepartment()).thenReturn(department);
        when(department.getId()).thenReturn(7L);
        when(department.getCode()).thenReturn("MCA");
        when(department.getName()).thenReturn("Computer Science & Applications");
        when(student.getAdmissionYear()).thenReturn((short) 2025);
        when(student.getGraduationYear()).thenReturn((short) 2029);
        when(student.getCurrentSemester()).thenReturn((short) 1);
        when(student.getCgpa()).thenReturn(new BigDecimal("8.50"));
    }

    @Test
    void returnsSafeIdentityAndProfileInformation() {
        MeResponse response = service.currentUser(authentication);

        assertEquals(42L, response.userId());
        assertEquals("25MCA001", response.enrollmentNumber());
        assertEquals("STUDENT", response.role());
        assertEquals("MCA", response.department().code());
        assertEquals((short) 2025, response.profile().admissionYear());
        assertEquals(new BigDecimal("8.50"), response.profile().cgpa());
    }

    @Test
    void updatesOnlyEditableProfileFields() {
        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "Updated Student", null, null, LocalDate.of(2005, 2, 3),
                "9876543210", "MCA", 2, new BigDecimal("9.10"), "Open to software roles");

        service.updateCurrentUser(authentication, request);

        verify(user).updateFullName("Updated Student");
        verify(student).updateProfile(
                (short) 2025, (short) 2029, LocalDate.of(2005, 2, 3), "9876543210", "MCA", (short) 2,
                new BigDecimal("9.10"), "Open to software roles", null);
        verify(users).save(user);
        verify(students).save(student);
    }

    @Test
    void rejectsInvalidProfileBeforePersistence() {
        ProfileUpdateRequest request = new ProfileUpdateRequest(
                null, null, null, LocalDate.now().plusDays(1), null, null, null,
                new BigDecimal("10.1"), null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateCurrentUser(authentication, request));

        assertEquals(400, exception.getStatusCode().value());
        verify(users, never()).save(any());
        verify(students, never()).save(any());
    }

    @Test
    void rejectsProfileUpdatesForNonStudents() {
        when(user.getRole()).thenReturn(UserRole.DTPO);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateCurrentUser(authentication,
                        new ProfileUpdateRequest("Name", null, null, null, null, null, null, null, null)));

        assertEquals(403, exception.getStatusCode().value());
        verify(users, never()).save(any());
        verify(students, never()).save(any());
    }

    @Test
    void rejectsSemesterOutsideConfiguredProgramDuration() {
        PlacementProgram program = org.mockito.Mockito.mock(PlacementProgram.class);
        when(programs.findByCodeIgnoreCase("MCA")).thenReturn(Optional.of(program));
        when(program.getDurationSemesters()).thenReturn((short) 4);
        when(student.getAcademicProgram()).thenReturn("MCA");
        CurrentUserService durationAwareService = new CurrentUserService(users, students, programs);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> durationAwareService.updateCurrentUser(authentication,
                        new ProfileUpdateRequest(null, null, null, null, null, "MCA", 5, null, null)));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Semester must be within your program duration", exception.getReason());
        verify(students, never()).save(any());
    }

    @Test
    void updatesSemesterWhenProgramDurationIsNotConfigured() {
        when(programs.findByCodeIgnoreCase("MCA")).thenReturn(Optional.empty());
        when(student.getAcademicProgram()).thenReturn("MCA");
        when(student.getCurrentSemester()).thenReturn((short) 2);
        CurrentUserService durationAwareService = new CurrentUserService(users, students, programs);

        durationAwareService.updateCurrentUser(authentication,
                new ProfileUpdateRequest(null, null, null, null, null,
                        "MCA", 3, null, null));

        verify(student).updateProfile(
                (short) 2025, (short) 2029, null, null, "MCA", (short) 3, new BigDecimal("8.50"), null, null);
        verify(students).save(student);
    }

    @Test
    void returnsAssignedUserDepartmentForStaffWithoutStudentProfile() {
        when(user.getRole()).thenReturn(UserRole.DTPO);
        when(user.getStudent()).thenReturn(null);
        when(user.getDepartment()).thenReturn(department);

        MeResponse response = service.currentUser(authentication);

        assertEquals("MCA", response.department().code());
        assertEquals(null, response.enrollmentNumber());
        assertEquals(null, response.profile());
    }

    @Test
    void returnsNullDepartmentForUniversityTpoWithoutAssignment() {
        when(user.getRole()).thenReturn(UserRole.UTPO);
        when(user.getStudent()).thenReturn(null);
        when(user.getDepartment()).thenReturn(null);

        MeResponse response = service.currentUser(authentication);

        assertEquals("UTPO", response.role());
        assertEquals(null, response.department());
        assertEquals(null, response.enrollmentNumber());
    }
}
