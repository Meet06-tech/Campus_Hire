package com.placementmanagement.api.student.entity;

import java.time.Instant;

import com.placementmanagement.api.auth.entity.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_change_otps")
public class PasswordChangeOtp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "password_change_otp_id") private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Column(name = "otp_hash", nullable = false, length = 100) private String otpHash;
    @Column(name = "requested_at", nullable = false, updatable = false) private Instant requestedAt = Instant.now();
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false) private int attempts;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "used_at") private Instant usedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PasswordOtpPurpose purpose = PasswordOtpPurpose.PASSWORD_CHANGE;
    @Column(name = "recovery_token_hash", length = 64) private String recoveryTokenHash;
    @Column(name = "recovery_token_expires_at") private Instant recoveryTokenExpiresAt;

    protected PasswordChangeOtp() {}
    public PasswordChangeOtp(UserAccount user, String otpHash, Instant expiresAt) {
        this.user = user; this.otpHash = otpHash; this.expiresAt = expiresAt;
    }
    public PasswordChangeOtp(UserAccount user, String otpHash, Instant expiresAt, PasswordOtpPurpose purpose) {
        this(user, otpHash, expiresAt); this.purpose = purpose;
    }
    public String getOtpHash() { return otpHash; }
    public UserAccount getUser() { return user; }
    public Long getId() { return id; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getAttempts() { return attempts; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getUsedAt() { return usedAt; }
    public PasswordOtpPurpose getPurpose() { return purpose; }
    public String getRecoveryTokenHash() { return recoveryTokenHash; }
    public Instant getRecoveryTokenExpiresAt() { return recoveryTokenExpiresAt; }
    public void incrementAttempt() { attempts++; }
    public void markVerified() { verifiedAt = Instant.now(); }
    public void markUsed() { usedAt = Instant.now(); }
    public void setRecoveryAuthorization(String tokenHash, Instant expiresAt) {
        recoveryTokenHash = tokenHash;
        recoveryTokenExpiresAt = expiresAt;
    }
}
