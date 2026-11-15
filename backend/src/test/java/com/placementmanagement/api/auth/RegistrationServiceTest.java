package com.placementmanagement.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.AuthDtos.RegisterRequest;
import com.placementmanagement.api.auth.dto.RegistrationDtos.RegistrationOtpRequest;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.DepartmentEmailCode;
import com.placementmanagement.api.auth.entity.PendingStudentRegistration;
import com.placementmanagement.api.auth.entity.Role;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.repository.DepartmentEmailCodeRepository;
import com.placementmanagement.api.auth.repository.PendingStudentRegistrationRepository;
import com.placementmanagement.api.auth.repository.RoleRepository;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.service.RegistrationOtpAttemptService;
import com.placementmanagement.api.auth.service.RegistrationService;
import com.placementmanagement.api.auth.service.UniversityEmailParser;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock UserAccountRepository users;
    @Mock StudentRepository students;
    @Mock DepartmentEmailCodeRepository codes;
    @Mock PendingStudentRegistrationRepository registrations;
    @Mock UniversityEmailParser parser;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder encoder;
    @Mock RegistrationOtpAttemptService attempts;
    @Mock ObjectProvider<JavaMailSender> mailSenders;
    @Mock JavaMailSender mailSender;
    @Mock Department department;
    @Mock DepartmentEmailCode mapping;
    @Mock Role studentRole;
    @Mock PendingStudentRegistration pending;

    private RegistrationService service() {
        return new RegistrationService(users, students, codes, registrations, parser, roles, encoder,
                attempts, mailSenders, "campushire@example.test");
    }

    @Test
    void beginStoresOnlyPendingDataAndDoesNotCreateAccount() {
        when(users.existsByStudentEnrollmentNumber("ENR-1")).thenReturn(false);
        when(users.existsByUniversityEmailIgnoreCase("25mca001@charusat.edu.in")).thenReturn(false);
        when(registrations.existsByEnrollmentNumber("ENR-1")).thenReturn(false);
        when(registrations.existsByUniversityEmail("25mca001@charusat.edu.in")).thenReturn(false);
        when(parser.extractCode("25mca001@charusat.edu.in")).thenReturn("MCA");
        when(codes.findByEmailCodeAndActiveTrue("MCA")).thenReturn(Optional.of(mapping));
        when(mapping.getDepartment()).thenReturn(department);
        when(department.isActive()).thenReturn(true);
        when(encoder.encode("safe-password")).thenReturn("bcrypt-hash");
        when(registrations.saveAndFlush(any(PendingStudentRegistration.class))).thenReturn(pending);
        when(pending.getId()).thenReturn(42L);
        when(mailSenders.getIfAvailable()).thenReturn(mailSender);

        assertEquals(42L, service().begin(new RegisterRequest(
                "Student One", "ENR-1", "25MCA001@charusat.edu.in", "safe-password", "safe-password"))
                .registrationId());
        verify(users, never()).save(any(UserAccount.class));
        verify(students, never()).save(any());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void invalidOtpDoesNotCreateAccount() {
        when(attempts.verify(42L, "000000")).thenReturn(RegistrationOtpAttemptService.Result.INVALID);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().verify(new RegistrationOtpRequest(42L, "000000")));

        assertEquals(400, error.getStatusCode().value());
        verify(users, never()).save(any(UserAccount.class));
        verify(students, never()).save(any());
    }
}
