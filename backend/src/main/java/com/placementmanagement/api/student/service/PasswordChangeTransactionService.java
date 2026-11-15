package com.placementmanagement.api.student.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.student.entity.PasswordChangeOtp;
import com.placementmanagement.api.student.repository.PasswordChangeOtpRepository;

/** Atomically consumes one verified OTP and updates the same authenticated user's password. */
@Service
public class PasswordChangeTransactionService {
    public enum Result { CHANGED, NOT_FOUND, EXPIRED, NOT_VERIFIED }

    private final UserAccountRepository users;
    private final PasswordChangeOtpRepository otps;

    public PasswordChangeTransactionService(UserAccountRepository users, PasswordChangeOtpRepository otps) {
        this.users = users;
        this.otps = otps;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result change(Long userId, String passwordHash) {
        UserAccount user = users.findByIdForUpdate(userId).orElse(null);
        if (user == null) return Result.NOT_FOUND;

        PasswordChangeOtp challenge = otps.findActiveForUpdate(userId).stream().findFirst().orElse(null);
        if (challenge == null) return Result.NOT_FOUND;
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            challenge.markUsed();
            return Result.EXPIRED;
        }
        if (challenge.getVerifiedAt() == null) return Result.NOT_VERIFIED;

        user.changePasswordHash(passwordHash);
        challenge.markUsed();
        return Result.CHANGED;
    }
}
