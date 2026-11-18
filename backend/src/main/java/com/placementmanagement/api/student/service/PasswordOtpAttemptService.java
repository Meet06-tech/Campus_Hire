package com.placementmanagement.api.student.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.placementmanagement.api.student.entity.PasswordChangeOtp;
import com.placementmanagement.api.student.entity.PasswordOtpPurpose;
import com.placementmanagement.api.student.repository.PasswordChangeOtpRepository;

/** Performs OTP state transitions while holding a row lock. It never throws for a failed attempt. */
@Service
public class PasswordOtpAttemptService {
    public enum Result { VERIFIED, INVALID, LOCKED, EXPIRED, ALREADY_VERIFIED, NOT_FOUND }

    private static final int MAX_ATTEMPTS = 5;
    private final PasswordChangeOtpRepository otps;
    private final PasswordEncoder encoder;

    public PasswordOtpAttemptService(PasswordChangeOtpRepository otps, PasswordEncoder encoder) {
        this.otps = otps;
        this.encoder = encoder;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result verify(Long userId, String suppliedOtp) {
        PasswordChangeOtp challenge = otps.findActiveForUpdate(userId).stream().findFirst().orElse(null);
        return evaluate(challenge, suppliedOtp);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result verify(Long userId, String suppliedOtp, PasswordOtpPurpose purpose) {
        PasswordChangeOtp challenge = otps.findActiveForUpdate(userId, purpose).stream().findFirst().orElse(null);
        return evaluate(challenge, suppliedOtp);
    }

    private Result evaluate(PasswordChangeOtp challenge, String suppliedOtp) {
        if (challenge == null) return Result.NOT_FOUND;
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            challenge.markUsed();
            return Result.EXPIRED;
        }
        if (challenge.getVerifiedAt() != null) return Result.ALREADY_VERIFIED;
        if (challenge.getAttempts() >= MAX_ATTEMPTS) {
            challenge.markUsed();
            return Result.LOCKED;
        }
        if (!encoder.matches(suppliedOtp, challenge.getOtpHash())) {
            challenge.incrementAttempt();
            if (challenge.getAttempts() >= MAX_ATTEMPTS) challenge.markUsed();
            return challenge.getAttempts() >= MAX_ATTEMPTS ? Result.LOCKED : Result.INVALID;
        }
        challenge.markVerified();
        return Result.VERIFIED;
    }
}
