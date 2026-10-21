package com.placementmanagement.api.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.AuthDtos.AuthResponse;
import com.placementmanagement.api.auth.dto.AuthDtos.RegisterRequest;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.DepartmentEmailCode;
import com.placementmanagement.api.auth.entity.Role;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.DepartmentEmailCodeRepository;
import com.placementmanagement.api.auth.repository.RoleRepository;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationTest {
    @Mock private UserAccountRepository users;
    @Mock private StudentRepository students;
    @Mock private DepartmentEmailCodeRepository departmentEmailCodes;
    @Mock private UniversityEmailParser emailParser;
    @Mock private RoleRepository roles;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService service;

    @Test
    void registersStudentUsingDepartmentResolvedFromEmailCode() {
        Department department = org.mockito.Mockito.mock(Department.class);
        DepartmentEmailCode mapping = org.mockito.Mockito.mock(DepartmentEmailCode.class);
        Role studentRole = org.mockito.Mockito.mock(Role.class);
        UserAccount persistedUser = org.mockito.Mockito.mock(UserAccount.class);
        Student persistedStudent = org.mockito.Mockito.mock(Student.class);

        when(users.existsByStudentEnrollmentNumber("ENR-1")).thenReturn(false);
        when(users.existsByUniversityEmail("25mca001@charusat.edu.in")).thenReturn(false);
        when(emailParser.extractCode("25mca001@charusat.edu.in")).thenReturn("MCA");
        when(departmentEmailCodes.findByEmailCodeAndActiveTrue("MCA")).thenReturn(Optional.of(mapping));
        when(mapping.getDepartment()).thenReturn(department);
        when(department.isActive()).thenReturn(true);
        when(roles.findByRoleCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("safe-password")).thenReturn("bcrypt-hash");
        when(users.save(any(UserAccount.class))).thenReturn(persistedUser);
        when(persistedUser.getId()).thenReturn(21L);
        when(persistedUser.getFullName()).thenReturn("Student One");
        when(persistedUser.getRole()).thenReturn(UserRole.STUDENT);
        when(persistedUser.getStudent()).thenReturn(persistedStudent);
        when(persistedStudent.getEnrollmentNumber()).thenReturn("ENR-1");
        when(jwtService.createToken(persistedUser)).thenReturn("opaque-test-token");

        AuthResponse response = service.register(new RegisterRequest(
                "Student One", "ENR-1", "25mca001@charusat.edu.in", "safe-password"));

        assertEquals("21", response.userId());
        assertEquals("STUDENT", response.role());
        verify(passwordEncoder).encode("safe-password");
        ArgumentCaptor<Student> student = ArgumentCaptor.forClass(Student.class);
        verify(students).save(student.capture());
        assertEquals("ENR-1", student.getValue().getEnrollmentNumber());
        assertEquals(department, student.getValue().getDepartment());
    }

    @Test
    void rejectsUnknownEmailCodeBeforePersistence() {
        when(users.existsByStudentEnrollmentNumber("ENR-1")).thenReturn(false);
        when(users.existsByUniversityEmail("25zzz001@charusat.edu.in")).thenReturn(false);
        when(emailParser.extractCode("25zzz001@charusat.edu.in")).thenReturn("ZZZ");
        when(departmentEmailCodes.findByEmailCodeAndActiveTrue("ZZZ")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.register(new RegisterRequest(
                        "Student One", "ENR-1", "25zzz001@charusat.edu.in", "safe-password")));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(users, never()).save(any());
        verifyNoInteractions(roles, passwordEncoder, students);
    }

    @Test
    void rejectsInactiveDepartmentBeforePersistence() {
        Department department = org.mockito.Mockito.mock(Department.class);
        DepartmentEmailCode mapping = org.mockito.Mockito.mock(DepartmentEmailCode.class);
        when(users.existsByStudentEnrollmentNumber("ENR-1")).thenReturn(false);
        when(users.existsByUniversityEmail("25mca001@charusat.edu.in")).thenReturn(false);
        when(emailParser.extractCode("25mca001@charusat.edu.in")).thenReturn("MCA");
        when(departmentEmailCodes.findByEmailCodeAndActiveTrue("MCA")).thenReturn(Optional.of(mapping));
        when(mapping.getDepartment()).thenReturn(department);
        when(department.isActive()).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.register(new RegisterRequest(
                        "Student One", "ENR-1", "25mca001@charusat.edu.in", "safe-password")));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(users, never()).save(any());
        verifyNoInteractions(roles, passwordEncoder, students);
    }

    @Test
    void rejectsDuplicateEnrollmentWithoutCreatingRecords() {
        when(users.existsByStudentEnrollmentNumber("ENR-1")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.register(new RegisterRequest(
                        "Student One", "ENR-1", "25mca001@charusat.edu.in", "safe-password")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(users, never()).save(any());
        verifyNoInteractions(departmentEmailCodes, emailParser, roles, passwordEncoder, students);
    }

    @Test
    void propagatesStudentPersistenceFailureForTransactionalRollback() {
        Department department = org.mockito.Mockito.mock(Department.class);
        DepartmentEmailCode mapping = org.mockito.Mockito.mock(DepartmentEmailCode.class);
        Role studentRole = org.mockito.Mockito.mock(Role.class);
        UserAccount persistedUser = org.mockito.Mockito.mock(UserAccount.class);

        when(users.existsByStudentEnrollmentNumber("ENR-1")).thenReturn(false);
        when(users.existsByUniversityEmail("25mca001@charusat.edu.in")).thenReturn(false);
        when(emailParser.extractCode("25mca001@charusat.edu.in")).thenReturn("MCA");
        when(departmentEmailCodes.findByEmailCodeAndActiveTrue("MCA")).thenReturn(Optional.of(mapping));
        when(mapping.getDepartment()).thenReturn(department);
        when(department.isActive()).thenReturn(true);
        when(roles.findByRoleCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("safe-password")).thenReturn("bcrypt-hash");
        when(users.save(any(UserAccount.class))).thenReturn(persistedUser);
        org.mockito.Mockito.doThrow(new RuntimeException("student insert failed"))
                .when(students).save(any(Student.class));

        assertThrows(RuntimeException.class, () -> service.register(new RegisterRequest(
                "Student One", "ENR-1", "25mca001@charusat.edu.in", "safe-password")));

        verify(users).save(any(UserAccount.class));
        verify(students).save(any(Student.class));
    }
}
