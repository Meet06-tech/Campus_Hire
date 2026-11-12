package com.placementmanagement.api.student;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.ChangePasswordRequest;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.ComplaintRequest;
import com.placementmanagement.api.student.service.PasswordChangeTransactionService;
import com.placementmanagement.api.student.service.PasswordOtpAttemptService;
import com.placementmanagement.api.student.service.PasswordOtpIssuanceService;
import com.placementmanagement.api.student.service.StudentFeatureService;

@ExtendWith(MockitoExtension.class)
class StudentFeatureServiceTest {
    @Mock StudentRepository students;
    @Mock PlacementAuthorizationService authorization;
    @Mock PasswordEncoder encoder;
    @Mock ObjectProvider<JavaMailSender> mailSenders;
    @Mock Authentication authentication;
    @Mock Student student;
    @Mock UserAccount user;
    @Mock Department department;
    @Mock PasswordOtpAttemptService otpAttempts;
    @Mock PasswordOtpIssuanceService otpIssuance;
    @Mock PasswordChangeTransactionService passwordChanges;

    private StudentFeatureService service() {
        return new StudentFeatureService(students, authorization, encoder, mailSenders,
                otpAttempts, otpIssuance, passwordChanges,
                "support@example.test", "campushire@example.test");
    }

    @Test
    void directoryRequiresStudentAndUsesSafeProjection() {
        when(authorization.student(authentication)).thenReturn(student);
        when(students.findStudentDirectory("mca")).thenReturn(List.of());
        service().directory(authentication, "mca");
        verify(students).findStudentDirectory("mca");
    }

    @Test
    void blankComplaintIsRejectedBeforeEmailDelivery() {
        when(authorization.student(authentication)).thenReturn(student);
        assertThrows(ResponseStatusException.class, () -> service().submitComplaint(authentication,
                new ComplaintRequest(" ", "Message")));
        verifyNoInteractions(mailSenders);
    }

    @Test
    void passwordMismatchIsRejectedBeforeOtpLookup() {
        when(authorization.student(authentication)).thenReturn(student);
        assertThrows(ResponseStatusException.class, () -> service().changePassword(authentication,
                new ChangePasswordRequest("new-password", "different-password")));
        verifyNoInteractions(otpAttempts, passwordChanges);
    }

    @Test
    void recentOtpRequestIsRejectedBeforeEmailDelivery() {
        when(authorization.student(authentication)).thenReturn(student);
        when(student.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(7L);
        when(otpIssuance.issue(eq(7L), any(), any())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service().requestOtp(authentication));
        verifyNoInteractions(mailSenders);
    }
}
