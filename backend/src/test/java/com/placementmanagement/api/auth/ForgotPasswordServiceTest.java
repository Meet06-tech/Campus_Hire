package com.placementmanagement.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.placementmanagement.api.auth.dto.ForgotPasswordDtos.VerifyForgotPasswordResponse;
import com.placementmanagement.api.auth.dto.ForgotPasswordDtos.ResetPasswordRequest;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.service.ForgotPasswordService;
import com.placementmanagement.api.student.entity.PasswordChangeOtp;
import com.placementmanagement.api.student.entity.PasswordOtpPurpose;
import com.placementmanagement.api.student.repository.PasswordChangeOtpRepository;
import com.placementmanagement.api.student.service.PasswordOtpAttemptService;
import com.placementmanagement.api.student.service.PasswordOtpIssuanceService;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {
    @Mock UserAccountRepository users;
    @Mock PasswordChangeOtpRepository otps;
    @Mock PasswordOtpIssuanceService issuance;
    @Mock PasswordOtpAttemptService attempts;
    @Mock PasswordEncoder encoder;
    @Mock ObjectProvider<JavaMailSender> mailSenders;
    @Mock JavaMailSender mailSender;

    private ForgotPasswordService service;

    @BeforeEach
    void setUp() {
        service = new ForgotPasswordService(users, otps, issuance, attempts, encoder, mailSenders,
                "campushire@example.test");
    }

    @Test
    void unknownEmailGetsGenericResponseWithoutIssuingOtp() {
        when(users.findByUniversityEmailIgnoreCase("unknown@example.test")).thenReturn(Optional.empty());

        assertEquals("If an account exists for that email, a verification code has been sent.",
                service.request(" UNKNOWN@example.test ").message());
        verify(issuance, org.mockito.Mockito.never()).issue(any(), any(), any(), any());
    }

    @Test
    void verifiedRecoveryOtpProducesShortLivedAuthorizationAndUsesRecoveryPurpose() {
        UserAccount user = new UserAccount("Student", "student@example.test", "old-hash");
        PasswordChangeOtp challenge = new PasswordChangeOtp(user, "otp-hash", Instant.now().plusSeconds(300),
                PasswordOtpPurpose.PASSWORD_RECOVERY);
        when(users.findByUniversityEmailIgnoreCase("student@example.test")).thenReturn(Optional.of(user));
        when(attempts.verify(any(), eq("123456"), eq(PasswordOtpPurpose.PASSWORD_RECOVERY)))
                .thenReturn(PasswordOtpAttemptService.Result.VERIFIED);
        when(otps.findActiveForUpdate(any(), eq(PasswordOtpPurpose.PASSWORD_RECOVERY))).thenReturn(List.of(challenge));

        VerifyForgotPasswordResponse response = service.verify("student@example.test", "123456");

        assertNotNull(response.recoveryToken());
        assertEquals(300, response.expiresInSeconds());
        assertNotNull(challenge.getRecoveryTokenHash());
        assertEquals(64, challenge.getRecoveryTokenHash().length());
    }

    @Test
    void resetConsumesRecoveryAuthorizationAndChangesOnlyTheBoundUserPassword() throws Exception {
        UserAccount user = new UserAccount("Student", "student@example.test", "old-hash");
        PasswordChangeOtp challenge = new PasswordChangeOtp(user, "otp-hash", Instant.now().plusSeconds(300),
                PasswordOtpPurpose.PASSWORD_RECOVERY);
        challenge.markVerified();
        String token = "known-recovery-token";
        String tokenHash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        challenge.setRecoveryAuthorization(tokenHash, Instant.now().plusSeconds(300));
        when(otps.findRecoveryForUpdate(eq(tokenHash), eq(PasswordOtpPurpose.PASSWORD_RECOVERY)))
                .thenReturn(Optional.of(challenge));
        when(encoder.encode("new-password")).thenReturn("new-bcrypt-hash");

        service.reset(new ResetPasswordRequest(token, "new-password", "new-password"));

        assertEquals("new-bcrypt-hash", user.getPasswordHash());
        assertNotNull(challenge.getUsedAt());
    }
}
