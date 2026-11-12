package com.placementmanagement.api.auth.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pending_student_registrations")
public class PendingStudentRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pending_registration_id")
    private Long id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "enrollment_number", nullable = false, unique = true, length = 80)
    private String enrollmentNumber;

    @Column(name = "university_email", nullable = false, unique = true, length = 255)
    private String universityEmail;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected PendingStudentRegistration() {}

    public PendingStudentRegistration(String fullName, String enrollmentNumber, String universityEmail,
            String passwordHash, Department department, String otpHash, Instant expiresAt) {
        this.fullName = fullName;
        this.enrollmentNumber = enrollmentNumber;
        this.universityEmail = universityEmail;
        this.passwordHash = passwordHash;
        this.department = department;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEnrollmentNumber() { return enrollmentNumber; }
    public String getUniversityEmail() { return universityEmail; }
    public String getPasswordHash() { return passwordHash; }
    public Department getDepartment() { return department; }
    public String getOtpHash() { return otpHash; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getAttempts() { return attempts; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getConsumedAt() { return consumedAt; }

    public void incrementAttempt() { attempts++; }
    public void markVerified() { verifiedAt = Instant.now(); }
    public void markConsumed() { consumedAt = Instant.now(); }
    public void replaceOtp(String otpHash, Instant requestedAt, Instant expiresAt) {
        this.otpHash = otpHash;
        this.requestedAt = requestedAt;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.verifiedAt = null;
    }
}
