package com.placementmanagement.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.AuthDtos.AuthResponse;
import com.placementmanagement.api.auth.dto.AuthDtos.LoginRequest;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Role;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.DepartmentEmailCodeRepository;
import com.placementmanagement.api.auth.repository.RoleRepository;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.security.JwtService;
import com.placementmanagement.api.auth.service.AuthService;
import com.placementmanagement.api.auth.service.StaffEmailAllowlist;
import com.placementmanagement.api.auth.service.UniversityEmailParser;

class AuthServiceAuthenticationTest {
    @Mock private UserAccountRepository users;
    @Mock private StudentRepository students;
    @Mock private DepartmentEmailCodeRepository departmentEmailCodes;
    @Mock private UniversityEmailParser emailParser;
    @Mock private RoleRepository roles;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserAccount staff;
    @Mock private Department department;
    @Mock private StaffEmailAllowlist staffEmailAllowlist;

    private AuthService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AuthService(users, students, departmentEmailCodes, emailParser, roles,
                passwordEncoder, jwtService, staffEmailAllowlist);
    }

    @Test
    void authenticatesStaffByEmailWithoutAStudentRecord() {
        when(users.findByUniversityEmailIgnoreCase("dtpo@charusat.edu.in")).thenReturn(Optional.of(staff));
        when(staffEmailAllowlist.isAllowed("DTPO@CHARUSAT.EDU.IN")).thenReturn(true);
        when(staff.isEnabled()).thenReturn(true);
        when(passwordEncoder.matches("staff-password", "bcrypt-hash")).thenReturn(true);
        when(staff.getPasswordHash()).thenReturn("bcrypt-hash");
        when(staff.getId()).thenReturn(9L);
        when(staff.getFullName()).thenReturn("Department TPO");
        when(staff.getStudent()).thenReturn(null);
        when(staff.getRole()).thenReturn(UserRole.DTPO);
        when(staff.getDepartment()).thenReturn(department);
        when(department.getId()).thenReturn(3L);
        when(department.getCode()).thenReturn("MCA");
        when(department.getName()).thenReturn("Computer Science & Applications");
        when(jwtService.createToken(staff)).thenReturn("staff-token");

        AuthResponse response = service.login(new LoginRequest("DTPO@CHARUSAT.EDU.IN", null, "staff-password"));

        assertEquals("staff-token", response.token());
        assertEquals("DTPO", response.role());
        assertNull(response.enrollmentNumber());
        assertEquals("MCA", response.department().code());
    }

    @Test
    void rejectsIncorrectStaffPassword() {
        when(users.findByUniversityEmailIgnoreCase("dtpo@charusat.edu.in")).thenReturn(Optional.of(staff));
        when(staffEmailAllowlist.isAllowed("dtpo@charusat.edu.in")).thenReturn(true);
        when(staff.isEnabled()).thenReturn(true);
        when(staff.getPasswordHash()).thenReturn("bcrypt-hash");
        when(passwordEncoder.matches("wrong", "bcrypt-hash")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("dtpo@charusat.edu.in", null, "wrong")));

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void keepsStudentEnrollmentLoginContract() {
        when(users.findByStudentEnrollmentNumber("ENR-42")).thenReturn(Optional.of(staff));
        when(staff.isEnabled()).thenReturn(true);
        when(staff.getPasswordHash()).thenReturn("bcrypt-hash");
        when(passwordEncoder.matches("student-password", "bcrypt-hash")).thenReturn(true);
        when(staff.getId()).thenReturn(42L);
        when(staff.getFullName()).thenReturn("Student");
        when(staff.getRole()).thenReturn(UserRole.STUDENT);
        when(staff.getStudent()).thenReturn(null);
        when(staff.getDepartment()).thenReturn(null);
        when(jwtService.createToken(staff)).thenReturn("student-token");

        AuthResponse response = service.login(new LoginRequest("ENR-42", "student-password"));

        assertEquals("student-token", response.token());
        assertEquals("STUDENT", response.role());
    }

    @Test
    void rejectsBothLoginIdentifiers() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("ENR-42", "password", "staff@charusat.ac.in")));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void rejectsUnknownStaffEmailAndDisabledAccount() {
        when(staffEmailAllowlist.isAllowed("missing@charusat.ac.in")).thenReturn(true);
        when(users.findByUniversityEmailIgnoreCase("missing@charusat.ac.in")).thenReturn(Optional.empty());

        ResponseStatusException unknown = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("missing@charusat.ac.in", null, "password")));
        assertEquals(401, unknown.getStatusCode().value());

        when(users.findByUniversityEmailIgnoreCase("disabled@charusat.ac.in")).thenReturn(Optional.of(staff));
        when(staffEmailAllowlist.isAllowed("disabled@charusat.ac.in")).thenReturn(true);
        when(staff.isEnabled()).thenReturn(false);
        ResponseStatusException disabled = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("disabled@charusat.ac.in", null, "password")));
        assertEquals(401, disabled.getStatusCode().value());
    }

    @Test
    void rejectsUnsupportedPersistedRoleWithoutServerError() {
        when(users.findByStudentEnrollmentNumber("ENR-42")).thenReturn(Optional.of(staff));
        when(staff.isEnabled()).thenReturn(true);
        when(staff.getPasswordHash()).thenReturn("bcrypt-hash");
        when(passwordEncoder.matches("password", "bcrypt-hash")).thenReturn(true);
        when(staff.getRole()).thenThrow(new IllegalArgumentException("unsupported role"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("ENR-42", "password")));

        assertEquals(401, exception.getStatusCode().value());
    }
}
