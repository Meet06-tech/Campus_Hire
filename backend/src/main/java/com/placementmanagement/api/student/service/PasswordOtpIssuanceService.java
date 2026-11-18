package com.placementmanagement.api.student.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.student.entity.PasswordChangeOtp;
import com.placementmanagement.api.student.entity.PasswordOtpPurpose;
import com.placementmanagement.api.student.repository.PasswordChangeOtpRepository;

/** Serializes OTP issuance per user using a database row lock. */
@Service
public class PasswordOtpIssuanceService {
    private final UserAccountRepository users;
    private final PasswordChangeOtpRepository otps;

    public PasswordOtpIssuanceService(UserAccountRepository users, PasswordChangeOtpRepository otps) {
        this.users = users;
        this.otps = otps;
    }

    @Transactional
    public Optional<PasswordChangeOtp> issue(Long userId, String otpHash, Instant now) {
        return issue(userId, otpHash, now, PasswordOtpPurpose.PASSWORD_CHANGE);
    }

    @Transactional
    public Optional<PasswordChangeOtp> issue(Long userId, String otpHash, Instant now, PasswordOtpPurpose purpose) {
        UserAccount user = users.findByIdForUpdate(userId).orElseThrow();
        if (otps.findTopByUser_IdOrderByRequestedAtDesc(userId)
                .filter(latest -> latest.getRequestedAt().plus(60, ChronoUnit.SECONDS).isAfter(now))
                .isPresent()) {
            return Optional.empty();
        }
        otps.findAllByUser_IdAndUsedAtIsNull(userId).forEach(PasswordChangeOtp::markUsed);
        return Optional.of(otps.save(new PasswordChangeOtp(user, otpHash, now.plus(5, ChronoUnit.MINUTES), purpose)));
    }
}
