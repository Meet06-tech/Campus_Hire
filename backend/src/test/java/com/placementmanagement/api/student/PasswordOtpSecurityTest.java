package com.placementmanagement.api.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.student.entity.PasswordChangeOtp;
import com.placementmanagement.api.student.repository.PasswordChangeOtpRepository;
import com.placementmanagement.api.student.service.PasswordChangeTransactionService;
import com.placementmanagement.api.student.service.PasswordOtpAttemptService;

@ExtendWith(MockitoExtension.class)
class PasswordOtpSecurityTest {
    @Mock PasswordChangeOtpRepository otps;
    @Mock PasswordEncoder encoder;
    @Mock UserAccountRepository users;

    private PasswordOtpAttemptService attempts;

    @BeforeEach
    void setUp() {
        attempts = new PasswordOtpAttemptService(otps, encoder);
    }

    @Test
    void failedAttemptsAreCountedAndFifthAttemptLocksChallenge() {
        PasswordChangeOtp challenge = challenge();
        when(otps.findActiveForUpdate(7L)).thenReturn(List.of(challenge));
        when(encoder.matches(any(String.class), any(String.class))).thenReturn(false);

        for (int i = 1; i < 5; i++) {
            assertEquals(PasswordOtpAttemptService.Result.INVALID, attempts.verify(7L, "000000"));
            assertEquals(i, challenge.getAttempts());
        }
        assertEquals(PasswordOtpAttemptService.Result.LOCKED, attempts.verify(7L, "000000"));
        assertEquals(5, challenge.getAttempts());
        assertNotNull(challenge.getUsedAt());
    }

    @Test
    void verifiedChallengeCannotBeVerifiedAgain() {
        PasswordChangeOtp challenge = challenge();
        when(otps.findActiveForUpdate(7L)).thenReturn(List.of(challenge));
        when(encoder.matches("123456", "hash")).thenReturn(true);

        assertEquals(PasswordOtpAttemptService.Result.VERIFIED, attempts.verify(7L, "123456"));
        assertEquals(PasswordOtpAttemptService.Result.ALREADY_VERIFIED, attempts.verify(7L, "123456"));
    }

    @Test
    void expiredChallengeIsConsumedAndRejected() {
        PasswordChangeOtp challenge = new PasswordChangeOtp(null, "hash", Instant.now().minusSeconds(1));
        when(otps.findActiveForUpdate(7L)).thenReturn(List.of(challenge));

        assertEquals(PasswordOtpAttemptService.Result.EXPIRED, attempts.verify(7L, "123456"));
        assertNotNull(challenge.getUsedAt());
    }

    @Test
    void passwordChangeAndOtpConsumptionUseOneTransactionServiceOperation() {
        UserAccount user = new UserAccount("Student", "student@example.test", "old-hash");
        PasswordChangeOtp challenge = challenge();
        challenge.markVerified();
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(otps.findActiveForUpdate(7L)).thenReturn(List.of(challenge));

        PasswordChangeTransactionService service = new PasswordChangeTransactionService(users, otps);
        assertEquals(PasswordChangeTransactionService.Result.CHANGED, service.change(7L, "new-hash"));
        assertEquals("new-hash", user.getPasswordHash());
        assertNotNull(challenge.getUsedAt());
    }

    private PasswordChangeOtp challenge() {
        return new PasswordChangeOtp(null, "hash", Instant.now().plusSeconds(300));
    }
}
