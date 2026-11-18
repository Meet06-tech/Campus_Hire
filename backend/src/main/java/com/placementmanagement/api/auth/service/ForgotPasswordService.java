package com.placementmanagement.api.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.ForgotPasswordDtos.ForgotPasswordResponse;
import com.placementmanagement.api.auth.dto.ForgotPasswordDtos.ResetPasswordRequest;
import com.placementmanagement.api.auth.dto.ForgotPasswordDtos.VerifyForgotPasswordResponse;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.student.entity.PasswordChangeOtp;
import com.placementmanagement.api.student.entity.PasswordOtpPurpose;
import com.placementmanagement.api.student.repository.PasswordChangeOtpRepository;
import com.placementmanagement.api.student.service.PasswordOtpAttemptService;
import com.placementmanagement.api.student.service.PasswordOtpIssuanceService;

@Service
public class ForgotPasswordService {
    private static final String GENERIC_MESSAGE = "If an account exists for that email, a verification code has been sent.";
    private static final long RECOVERY_TOKEN_SECONDS = 300;

    private final SecureRandom random = new SecureRandom();
    private final UserAccountRepository users;
    private final PasswordChangeOtpRepository otps;
    private final PasswordOtpIssuanceService issuance;
    private final PasswordOtpAttemptService attempts;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String fromEmail;

    public ForgotPasswordService(UserAccountRepository users, PasswordChangeOtpRepository otps,
            PasswordOtpIssuanceService issuance, PasswordOtpAttemptService attempts,
            PasswordEncoder encoder, ObjectProvider<JavaMailSender> mailSenders,
            @Value("${app.support.from-email}") String fromEmail) {
        this.users = users;
        this.otps = otps;
        this.issuance = issuance;
        this.attempts = attempts;
        this.encoder = encoder;
        this.mailSenders = mailSenders;
        this.fromEmail = fromEmail;
    }

    @Transactional
    public ForgotPasswordResponse request(String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null) return new ForgotPasswordResponse(GENERIC_MESSAGE);
        UserAccount user = users.findByUniversityEmailIgnoreCase(normalized).orElse(null);
        if (user == null || !user.isEnabled()) return new ForgotPasswordResponse(GENERIC_MESSAGE);

        String otp = "%06d".formatted(random.nextInt(1_000_000));
        PasswordChangeOtp challenge = issuance.issue(user.getId(), encoder.encode(otp), Instant.now(),
                PasswordOtpPurpose.PASSWORD_RECOVERY)
                .orElseThrow(() -> bad("Please wait before requesting another OTP"));
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null) {
            challenge.markUsed();
            throw unavailable("Password email service is not configured");
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(user.getUniversityEmail());
        mail.setFrom(fromEmail);
        mail.setSubject("CampusHire password recovery OTP");
        mail.setText("Your CampusHire password recovery OTP is " + otp + ". It expires in 5 minutes.");
        try {
            sender.send(mail);
        } catch (RuntimeException exception) {
            challenge.markUsed();
            throw unavailable("The password recovery email could not be delivered. Please try again later");
        }
        return new ForgotPasswordResponse(GENERIC_MESSAGE);
    }

    @Transactional
    public VerifyForgotPasswordResponse verify(String email, String otp) {
        String normalized = normalizeEmail(email);
        if (normalized == null || otp == null || !otp.matches("\\d{6}")) throw bad("The OTP is invalid or expired");
        UserAccount user = users.findByUniversityEmailIgnoreCase(normalized).orElse(null);
        if (user == null) throw bad("The OTP is invalid or expired");
        PasswordOtpAttemptService.Result result = attempts.verify(user.getId(), otp, PasswordOtpPurpose.PASSWORD_RECOVERY);
        if (result != PasswordOtpAttemptService.Result.VERIFIED) throw bad("The OTP is invalid or expired");

        PasswordChangeOtp challenge = otps.findActiveForUpdate(user.getId(), PasswordOtpPurpose.PASSWORD_RECOVERY)
                .stream().findFirst().orElseThrow(() -> bad("The OTP is invalid or expired"));
        String token = newToken();
        challenge.setRecoveryAuthorization(hash(token), Instant.now().plusSeconds(RECOVERY_TOKEN_SECONDS));
        return new VerifyForgotPasswordResponse(token, RECOVERY_TOKEN_SECONDS);
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        if (request == null || request.recoveryToken() == null || request.recoveryToken().isBlank()) {
            throw bad("Recovery verification is required");
        }
        validatePassword(request.newPassword(), request.confirmPassword());
        PasswordChangeOtp challenge = otps.findRecoveryForUpdate(hash(request.recoveryToken()), PasswordOtpPurpose.PASSWORD_RECOVERY)
                .orElseThrow(() -> bad("The recovery authorization is invalid or expired"));
        Instant now = Instant.now();
        if (challenge.getVerifiedAt() == null || challenge.getRecoveryTokenExpiresAt() == null
                || challenge.getRecoveryTokenExpiresAt().isBefore(now)
                || challenge.getExpiresAt().isBefore(now)) {
            challenge.markUsed();
            throw bad("The recovery authorization is invalid or expired");
        }
        challenge.getUser().changePasswordHash(encoder.encode(request.newPassword()));
        challenge.markUsed();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String value = email.trim().toLowerCase(Locale.ROOT);
        return value.matches("^[^@\\s]+@[^@\\s]+$") ? value : null;
    }

    private void validatePassword(String password, String confirmation) {
        if (password == null || password.length() < 8) throw bad("Password must be at least 8 characters");
        if (!password.equals(confirmation)) throw bad("Passwords do not match");
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException unavailable(String message) { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message); }
}
