package com.placementmanagement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.TransactionSystemException;

import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.PlacementManagementApplication;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.student.entity.PasswordChangeOtp;
import com.placementmanagement.api.student.repository.PasswordChangeOtpRepository;
import com.placementmanagement.api.student.service.PasswordChangeTransactionService;
import com.placementmanagement.api.student.service.PasswordOtpAttemptService;
import com.placementmanagement.api.student.service.PasswordOtpIssuanceService;

@SpringBootTest(classes = PlacementManagementApplication.class)
@ActiveProfiles("integration-test")
@Import(IntegrationDatabaseGuard.class)
class PasswordOtpPostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository users;
    @Autowired PasswordChangeOtpRepository otps;
    @Autowired PasswordEncoder encoder;
    @Autowired PasswordOtpAttemptService attempts;
    @Autowired PasswordOtpIssuanceService issuance;
    @Autowired PasswordChangeTransactionService passwordChanges;

    private Long userId;
    private String email;

    @BeforeEach
    void createIsolatedUser() {
        email = "otp-test-" + UUID.randomUUID() + "@example.invalid";
        jdbc.update("insert into roles(role_code, role_name) values ('STUDENT', 'Integration Student') on conflict (role_code) do nothing");
        Long roleId = jdbc.queryForObject("select role_id from roles where role_code = 'STUDENT'", Long.class);
        jdbc.update("insert into users(full_name, university_email, password_hash, role_id, is_active, created_at) values (?, ?, ?, ?, true, current_timestamp)",
                "OTP Integration Student", email, encoder.encode("old-password"), roleId);
        userId = jdbc.queryForObject("select user_id from users where university_email = ?", Long.class, email);
    }

    @AfterEach
    void cleanIsolatedUser() {
        jdbc.update("delete from password_change_otps where user_id = ?", userId);
        jdbc.update("delete from users where user_id = ?", userId);
    }

    @Test
    void failedAttemptsPersistAndFifthAttemptConsumesChallenge() {
        PasswordChangeOtp challenge = createOtp(Instant.now().plusSeconds(300));
        for (int i = 1; i <= 4; i++) {
            assertEquals(PasswordOtpAttemptService.Result.INVALID, attempts.verify(userId, "000000"));
            assertEquals(i, otps.findById(challenge.getId()).orElseThrow().getAttempts());
        }
        assertEquals(PasswordOtpAttemptService.Result.LOCKED, attempts.verify(userId, "000000"));
        PasswordChangeOtp persisted = otps.findById(challenge.getId()).orElseThrow();
        assertEquals(5, persisted.getAttempts());
        assertNotNull(persisted.getUsedAt());
        assertEquals(PasswordOtpAttemptService.Result.NOT_FOUND, attempts.verify(userId, "000000"));
    }

    @Test
    void concurrentIssuanceAllowsOnlyOneActiveChallengeForOneUser() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = pool.submit(() -> issueAfter(start));
            Future<Boolean> second = pool.submit(() -> issueAfter(start));
            start.countDown();
            assertEquals(1, List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)).stream()
                    .filter(Boolean::booleanValue).count());
            assertEquals(1, otps.findAllByUser_IdAndUsedAtIsNull(userId).size());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void differentUsersCanIssueIndependently() throws Exception {
        Long otherUserId = createSecondUser();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = pool.submit(() -> { await(start); issuance.issue(userId, "hash-a", Instant.now()); });
            Future<?> second = pool.submit(() -> { await(start); issuance.issue(otherUserId, "hash-b", Instant.now()); });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            assertEquals(1, otps.findAllByUser_IdAndUsedAtIsNull(userId).size());
            assertEquals(1, otps.findAllByUser_IdAndUsedAtIsNull(otherUserId).size());
            jdbc.update("delete from password_change_otps where user_id = ?", otherUserId);
            jdbc.update("delete from users where user_id = ?", otherUserId);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentPasswordChangesConsumeOneVerifiedOtp() throws Exception {
        PasswordChangeOtp challenge = createOtp(Instant.now().plusSeconds(300));
        assertEquals(PasswordOtpAttemptService.Result.VERIFIED, attempts.verify(userId, "123456"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<PasswordChangeTransactionService.Result> first = pool.submit(() -> changeAfter(start, "hash-a"));
            Future<PasswordChangeTransactionService.Result> second = pool.submit(() -> changeAfter(start, "hash-b"));
            start.countDown();
            List<PasswordChangeTransactionService.Result> results = List.of(
                    first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(result -> result == PasswordChangeTransactionService.Result.CHANGED).count());
            assertEquals(1, results.stream().filter(result -> result != PasswordChangeTransactionService.Result.CHANGED).count());
            assertNotNull(otps.findById(challenge.getId()).orElseThrow().getUsedAt());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void successfulPasswordChangeStoresHashAcceptedByTheExistingEncoder() {
        createOtp(Instant.now().plusSeconds(300));
        assertEquals(PasswordOtpAttemptService.Result.VERIFIED, attempts.verify(userId, "123456"));

        String newHash = encoder.encode("new-password");
        assertEquals(PasswordChangeTransactionService.Result.CHANGED, passwordChanges.change(userId, newHash));
        String persistedHash = users.findById(userId).orElseThrow().getPasswordHash();
        assertTrue(encoder.matches("new-password", persistedHash));
        assertTrue(!encoder.matches("old-password", persistedHash));
    }

    @Test
    void expiryReplayAndRollbackAreEnforced() {
        PasswordChangeOtp expired = createOtp(Instant.now().minusSeconds(1));
        assertEquals(PasswordOtpAttemptService.Result.EXPIRED, attempts.verify(userId, "123456"));
        assertNotNull(otps.findById(expired.getId()).orElseThrow().getUsedAt());

        PasswordChangeOtp verified = createOtp(Instant.now().plusSeconds(300));
        assertEquals(PasswordOtpAttemptService.Result.VERIFIED, attempts.verify(userId, "123456"));
        assertEquals(PasswordOtpAttemptService.Result.ALREADY_VERIFIED, attempts.verify(userId, "123456"));
        assertThrows(RuntimeException.class, () -> passwordChanges.change(userId, null));
        PasswordChangeOtp afterFailure = otps.findById(verified.getId()).orElseThrow();
        assertNotNull(afterFailure.getVerifiedAt());
        assertNull(afterFailure.getUsedAt());
        assertTrue(encoder.matches("old-password", users.findById(userId).orElseThrow().getPasswordHash()));
    }

    private PasswordChangeOtp createOtp(Instant expiry) {
        return otps.saveAndFlush(new PasswordChangeOtp(users.findById(userId).orElseThrow(),
                encoder.encode("123456"), expiry));
    }

    private boolean issueAfter(CountDownLatch start) {
        await(start);
        return issuance.issue(userId, "hash", Instant.now()).isPresent();
    }

    private PasswordChangeTransactionService.Result changeAfter(CountDownLatch start, String hash) {
        await(start);
        return passwordChanges.change(userId, hash);
    }

    private void await(CountDownLatch start) {
        try { start.await(10, TimeUnit.SECONDS); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
    }

    private Long createSecondUser() {
        String secondEmail = "otp-test-" + UUID.randomUUID() + "@example.invalid";
        Long roleId = jdbc.queryForObject("select role_id from roles where role_code = 'STUDENT'", Long.class);
        jdbc.update("insert into users(full_name, university_email, password_hash, role_id, is_active, created_at) values (?, ?, ?, ?, true, current_timestamp)",
                "OTP Integration Student 2", secondEmail, "hash", roleId);
        return jdbc.queryForObject("select user_id from users where university_email = ?", Long.class, secondEmail);
    }
}
