package com.placementmanagement.api.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
@SecondaryTable(name = "students", pkJoinColumns = @PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id"))
public class UserAccount {

    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "enrollment_number", table = "students", nullable = false, unique = true, length = 80)
    private String enrollmentNumber;

    @Column(name = "university_email", nullable = false, unique = true)
    private String universityEmail;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected UserAccount() {
    }

    public UserAccount(String fullName, String enrollmentNumber, String universityEmail, String passwordHash) {
        this.fullName = fullName;
        this.enrollmentNumber = enrollmentNumber;
        this.universityEmail = universityEmail;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEnrollmentNumber() { return enrollmentNumber; }
    public String getUniversityEmail() { return universityEmail; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return UserRole.valueOf(role.getRoleCode()); }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }

    void setRole(Role role) { this.role = role; }
}

