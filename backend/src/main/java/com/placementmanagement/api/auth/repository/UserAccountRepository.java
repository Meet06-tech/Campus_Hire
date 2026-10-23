package com.placementmanagement.api.auth.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placementmanagement.api.auth.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByStudentEnrollmentNumber(String enrollmentNumber);
    Optional<UserAccount> findByUniversityEmailIgnoreCase(String universityEmail);
    boolean existsByStudentEnrollmentNumber(String enrollmentNumber);
    boolean existsByUniversityEmail(String universityEmail);
    boolean existsByUniversityEmailIgnoreCase(String universityEmail);
    List<UserAccount> findAllByRole_RoleCodeOrderByFullName(String roleCode);
}
