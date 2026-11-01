package com.placementmanagement.tools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Role;
import com.placementmanagement.api.auth.repository.DepartmentRepository;
import com.placementmanagement.api.auth.repository.RoleRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.service.StaffEmailAllowlist;

@ExtendWith(MockitoExtension.class)
class StaffProvisioningServiceTest {
    @Mock private UserAccountRepository users;
    @Mock private RoleRepository roles;
    @Mock private DepartmentRepository departments;
    @Mock private PasswordEncoder encoder;
    @Mock private StaffEmailAllowlist allowlist;
    @Mock private Role utpoRole;
    @Mock private Role dtpoRole;
    @Mock private Department department;

    @InjectMocks private StaffProvisioningService service;

    @Test
    void validatesBeforeWritingAndClearsPasswords() {
        StaffProvisioningRequest request = request();
        when(allowlist.isAllowed("utpo@charusat.ac.in")).thenReturn(true);
        when(allowlist.isAllowed("dtpo@charusat.ac.in")).thenReturn(true);
        when(users.existsByUniversityEmailIgnoreCase(any())).thenReturn(false);
        when(roles.findByRoleCode("UTPO")).thenReturn(Optional.of(utpoRole));
        when(roles.findByRoleCode("DTPO")).thenReturn(Optional.of(dtpoRole));
        when(departments.findById(2L)).thenReturn(Optional.of(department));
        when(department.isActive()).thenReturn(true);
        when(department.getId()).thenReturn(2L);
        when(encoder.encode(any())).thenReturn("synthetic-test-hash");

        service.provision(request);

        ArgumentCaptor<com.placementmanagement.api.auth.entity.UserAccount> saved =
                ArgumentCaptor.forClass(com.placementmanagement.api.auth.entity.UserAccount.class);
        verify(users, times(2)).save(saved.capture());
        List<com.placementmanagement.api.auth.entity.UserAccount> accounts = saved.getAllValues();
        assertNull(accounts.get(0).getDepartment());
        assertEquals(2L, accounts.get(1).getDepartment().getId());
        verify(encoder).encode("utpo-password");
        verify(encoder).encode("dtpo-password");
        assertArrayEquals(new char[13], request.utpoPassword());
        assertArrayEquals(new char[13], request.dtpoPassword());
    }

    @Test
    void rejectsCaseInsensitiveDuplicateBeforeRoleLookupOrWrites() {
        StaffProvisioningRequest request = request();
        when(allowlist.isAllowed("utpo@charusat.ac.in")).thenReturn(true);
        when(allowlist.isAllowed("dtpo@charusat.ac.in")).thenReturn(true);
        when(users.existsByUniversityEmailIgnoreCase("utpo@charusat.ac.in")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.provision(request));
        verify(roles, never()).findByRoleCode(any());
        verify(users, never()).save(any());
    }

    @Test
    void rejectsInactiveDepartmentBeforeWrites() {
        StaffProvisioningRequest request = request();
        when(allowlist.isAllowed(any())).thenReturn(true);
        when(users.existsByUniversityEmailIgnoreCase(any())).thenReturn(false);
        when(roles.findByRoleCode("UTPO")).thenReturn(Optional.of(utpoRole));
        when(roles.findByRoleCode("DTPO")).thenReturn(Optional.of(dtpoRole));
        when(departments.findById(2L)).thenReturn(Optional.of(department));
        when(department.isActive()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.provision(request));
        verify(users, never()).save(any());
    }

    @Test
    void rejectsEmailsThatNormalizeToTheSameAddress() {
        StaffProvisioningRequest request = new StaffProvisioningRequest(
                "U.TPO", "Staff@CHARUSAT.AC.IN", "utpo-password".toCharArray(),
                "D.TPO", " staff@charusat.ac.in ", "dtpo-password".toCharArray());

        assertThrows(IllegalArgumentException.class, () -> service.provision(request));
        verify(users, never()).save(any());
    }

    @Test
    void rejectsMissingRoleBeforeWriting() {
        StaffProvisioningRequest request = request();
        when(allowlist.isAllowed(any())).thenReturn(true);
        when(users.existsByUniversityEmailIgnoreCase(any())).thenReturn(false);
        when(roles.findByRoleCode("UTPO")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.provision(request));
        verify(users, never()).save(any());
    }

    @Test
    void rejectsMissingDepartmentBeforeWriting() {
        StaffProvisioningRequest request = request();
        when(allowlist.isAllowed(any())).thenReturn(true);
        when(users.existsByUniversityEmailIgnoreCase(any())).thenReturn(false);
        when(roles.findByRoleCode("UTPO")).thenReturn(Optional.of(utpoRole));
        when(roles.findByRoleCode("DTPO")).thenReturn(Optional.of(dtpoRole));
        when(departments.findById(2L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.provision(request));
        verify(users, never()).save(any());
    }

    @Test
    void propagatesSecondInsertFailureForTransactionRollback() {
        StaffProvisioningRequest request = request();
        when(allowlist.isAllowed(any())).thenReturn(true);
        when(users.existsByUniversityEmailIgnoreCase(any())).thenReturn(false);
        when(roles.findByRoleCode("UTPO")).thenReturn(Optional.of(utpoRole));
        when(roles.findByRoleCode("DTPO")).thenReturn(Optional.of(dtpoRole));
        when(departments.findById(2L)).thenReturn(Optional.of(department));
        when(department.isActive()).thenReturn(true);
        when(encoder.encode(any())).thenReturn("synthetic-test-hash");
        when(users.save(any())).thenReturn(null).thenThrow(new IllegalStateException("synthetic insert failure"));

        assertThrows(IllegalStateException.class, () -> service.provision(request));
        verify(users, times(2)).save(any());
    }

    private StaffProvisioningRequest request() {
        return new StaffProvisioningRequest("U.TPO", "UTPO@CHARUSAT.AC.IN",
                "utpo-password".toCharArray(), "D.TPO", "DTPO@CHARUSAT.AC.IN",
                "dtpo-password".toCharArray());
    }
}
