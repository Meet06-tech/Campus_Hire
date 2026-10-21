package com.placementmanagement.api.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placementmanagement.api.auth.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByStudentEnrollmentNumber(String enrollmentNumber);
    boolean existsByStudentEnrollmentNumber(String enrollmentNumber);
    boolean existsByUniversityEmail(String universityEmail);
}
