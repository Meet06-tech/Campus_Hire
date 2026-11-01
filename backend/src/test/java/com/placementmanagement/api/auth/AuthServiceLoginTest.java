package com.placementmanagement.api.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.AuthDtos.AuthResponse;
import com.placementmanagement.api.auth.dto.AuthDtos.LoginRequest;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Student;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {
    @Mock UserAccountRepository users;
    @Mock StudentRepository students;
    @Mock DepartmentEmailCodeRepository departmentEmailCodes;
    @Mock UniversityEmailParser emailParser;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @Mock StaffEmailAllowlist allowlist;
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, students, departmentEmailCodes, emailParser, roles, encoder, jwt, allowlist);
    }

    @Test
    void studentLoginKeepsEnrollmentContract() {
        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(true);
        when(user.getId()).thenReturn(10L);
        when(user.getFullName()).thenReturn("Test Student");
        when(user.getPasswordHash()).thenReturn("bcrypt-hash");
        when(user.getRole()).thenReturn(UserRole.STUDENT);
        Student student = student("ENR-1");
        when(user.getStudent()).thenReturn(student);
        when(user.getDepartment()).thenReturn(null);
        when(users.findByStudentEnrollmentNumber("ENR-1")).thenReturn(Optional.of(user));
        when(encoder.matches("password", "bcrypt-hash")).thenReturn(true);
        when(jwt.createToken(user)).thenReturn("test-token");

        AuthResponse response = service.login(new LoginRequest(null, "ENR-1", "password"));

        assertEquals("STUDENT", response.role());
        assertEquals("ENR-1", response.enrollmentNumber());
        verify(users).findByStudentEnrollmentNumber("ENR-1");
        verify(users, never()).findByUniversityEmailIgnoreCase(anyString());
    }

    @Test
    void staffLoginUsesEmailAndDoesNotRequireStudent() {
        when(allowlist.isAllowed("dtpo@charusat.ac.in")).thenReturn(true);
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(2L);
        when(department.getCode()).thenReturn("MCA");
        when(department.getName()).thenReturn("Computer Science & Applications");
        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(true);
        when(user.getId()).thenReturn(10L);
        when(user.getFullName()).thenReturn("Test DTPO");
        when(user.getPasswordHash()).thenReturn("bcrypt-hash");
        when(user.getRole()).thenReturn(UserRole.DTPO);
        when(user.getStudent()).thenReturn(null);
        when(user.getDepartment()).thenReturn(department);
        when(users.findByUniversityEmailIgnoreCase("dtpo@charusat.ac.in")).thenReturn(Optional.of(user));
        when(encoder.matches("password", "bcrypt-hash")).thenReturn(true);
        when(jwt.createToken(user)).thenReturn("test-token");

        AuthResponse response = service.login(new LoginRequest("dtpo@charusat.ac.in", null, "password"));

        assertEquals("DTPO", response.role());
        assertNull(response.enrollmentNumber());
        assertEquals(2L, response.department().id());
    }

    @Test
    void utpoLoginUsesEmailWithoutStudentOrDepartment() {
        when(allowlist.isAllowed("utpo@charusat.ac.in")).thenReturn(true);
        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(true);
        when(user.getId()).thenReturn(11L);
        when(user.getFullName()).thenReturn("Test UTPO");
        when(user.getPasswordHash()).thenReturn("bcrypt-hash");
        when(user.getRole()).thenReturn(UserRole.UTPO);
        when(user.getStudent()).thenReturn(null);
        when(user.getDepartment()).thenReturn(null);
        when(users.findByUniversityEmailIgnoreCase("utpo@charusat.ac.in")).thenReturn(Optional.of(user));
        when(encoder.matches("password", "bcrypt-hash")).thenReturn(true);
        when(jwt.createToken(user)).thenReturn("test-token");

        AuthResponse response = service.login(new LoginRequest("utpo@charusat.ac.in", null, "password"));

        assertEquals("UTPO", response.role());
        assertNull(response.enrollmentNumber());
        assertNull(response.department());
    }

    @Test
    void invalidPasswordIsRejected() {
        when(allowlist.isAllowed("utpo@charusat.ac.in")).thenReturn(true);
        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn("bcrypt-hash");
        when(users.findByUniversityEmailIgnoreCase("utpo@charusat.ac.in")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong", "bcrypt-hash")).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("utpo@charusat.ac.in", null, "wrong")));

        assertEquals(401, error.getStatusCode().value());
    }

    @Test
    void bothIdentifiersAreRejected() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("staff@charusat.ac.in", "ENR-1", "password")));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(users, encoder, jwt);
    }

    private Student student(String enrollment) {
        Student student = mock(Student.class);
        when(student.getEnrollmentNumber()).thenReturn(enrollment);
        return student;
    }
}
