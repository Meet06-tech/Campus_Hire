package com.placementmanagement.api.auth.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.placementmanagement.api.auth.entity.PendingStudentRegistration;
import com.placementmanagement.api.auth.repository.PendingStudentRegistrationRepository;

@Service
public class RegistrationOtpAttemptService {
    public enum Result { VERIFIED, INVALID, LOCKED, EXPIRED, ALREADY_VERIFIED, NOT_FOUND }
    private static final int MAX_ATTEMPTS = 5;
    private final PendingStudentRegistrationRepository registrations;
    private final PasswordEncoder encoder;

    public RegistrationOtpAttemptService(PendingStudentRegistrationRepository registrations, PasswordEncoder encoder) {
        this.registrations = registrations;
        this.encoder = encoder;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result verify(Long registrationId, String otp) {
        PendingStudentRegistration registration = registrations.findByIdForUpdateForVerification(registrationId).orElse(null);
        if (registration == null || registration.getConsumedAt() != null) return Result.NOT_FOUND;
        if (registration.getExpiresAt().isBefore(Instant.now())) {
            registration.markConsumed();
            return Result.EXPIRED;
        }
        if (registration.getVerifiedAt() != null) return Result.ALREADY_VERIFIED;
        if (registration.getAttempts() >= MAX_ATTEMPTS) {
            registration.markConsumed();
            return Result.LOCKED;
        }
        if (!encoder.matches(otp, registration.getOtpHash())) {
            registration.incrementAttempt();
            if (registration.getAttempts() >= MAX_ATTEMPTS) registration.markConsumed();
            return registration.getAttempts() >= MAX_ATTEMPTS ? Result.LOCKED : Result.INVALID;
        }
        registration.markVerified();
        return Result.VERIFIED;
    }
}
