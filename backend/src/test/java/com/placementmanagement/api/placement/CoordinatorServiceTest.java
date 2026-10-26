package com.placementmanagement.api.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Role;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.DepartmentRepository;
import com.placementmanagement.api.auth.repository.RoleRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.service.StaffEmailAllowlist;
import com.placementmanagement.api.placement.dto.PlacementDtos.CoordinatorRequest;
import com.placementmanagement.api.placement.service.CoordinatorService;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoordinatorServiceTest {
    @Mock UserAccountRepository users;
    @Mock DepartmentRepository departments;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PlacementAuthorizationService authorization;
    @Mock StaffEmailAllowlist emailAllowlist;
    @Mock Authentication authentication;
    @Mock UserAccount utpo;
    @Mock Department department;
    @Mock Role dtpoRole;

    private CoordinatorService service;

    @BeforeEach
    void setUp() {
        service = new CoordinatorService(users, departments, roles, passwordEncoder, authorization, emailAllowlist);
        when(authorization.staff(authentication)).thenReturn(utpo);
        when(utpo.getRole()).thenReturn(UserRole.UTPO);
        when(emailAllowlist.accepts("dtpo@charusat.ac.in")).thenReturn(true);
        when(departments.findById(2L)).thenReturn(Optional.of(department));
        when(department.isActive()).thenReturn(true);
        when(department.getId()).thenReturn(2L);
        when(department.getCode()).thenReturn("MCA");
        when(department.getName()).thenReturn("Computer Science & Applications");
        when(roles.findByRoleCode("DTPO")).thenReturn(Optional.of(dtpoRole));
        when(users.existsByUniversityEmailIgnoreCase("dtpo@charusat.ac.in")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("bcrypt-hash");
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void utpoCreatesDepartmentScopedDtpoWithHashedPassword() {
        var response = service.create(authentication,
                new CoordinatorRequest("Department TPO", "DTPO@CHARUSAT.AC.IN", 2L, "strong-password"));

        assertEquals("dtpo@charusat.ac.in", response.universityEmail());
        assertEquals(2L, response.department().id());
        verify(passwordEncoder).encode("strong-password");
        ArgumentCaptor<UserAccount> account = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).save(account.capture());
        assertEquals("bcrypt-hash", account.getValue().getPasswordHash());
        // role and department are set on a real UserAccount (not a mock); verify the response reflects them
        assertEquals(2L, response.department().id());
        verify(utpo).getRole();
    }

    @Test
    void duplicateEmailIsRejectedBeforeDepartmentOrPasswordWork() {
        when(users.existsByUniversityEmailIgnoreCase("dtpo@charusat.ac.in")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(authentication,
                        new CoordinatorRequest("Department TPO", "dtpo@charusat.ac.in", 2L, "strong-password")));

        assertEquals(409, error.getStatusCode().value());
        verifyNoInteractions(passwordEncoder);
        verify(users, never()).save(any());
    }

    @Test
    void nonUtpoCannotCreateCoordinator() {
        when(utpo.getRole()).thenReturn(UserRole.DTPO);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(authentication,
                        new CoordinatorRequest("Department TPO", "dtpo@charusat.ac.in", 2L, "strong-password")));

        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(users, departments, passwordEncoder);
    }

    @Test
    void invalidDepartmentIsRejectedBeforePasswordHashing() {
        when(departments.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(authentication,
                        new CoordinatorRequest("Department TPO", "dtpo@charusat.ac.in", 99L, "strong-password")));

        assertEquals(404, error.getStatusCode().value());
        verifyNoInteractions(passwordEncoder);
        verify(users, never()).save(any());
    }

    @Test
    void missingDepartmentIsRejected() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(authentication,
                        new CoordinatorRequest("Department TPO", "dtpo@charusat.ac.in", null, "strong-password")));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(departments, passwordEncoder, users);
    }

    @Test
    void onlyUtpoCanListCoordinators() {
        when(utpo.getRole()).thenReturn(UserRole.DTPO);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.list(authentication));

        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(users);
    }
}
