package com.placementmanagement.api.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEnrollmentNumber(String enrollmentNumber);
    boolean existsByEnrollmentNumber(String enrollmentNumber);
    boolean existsByUniversityEmail(String universityEmail);
}

