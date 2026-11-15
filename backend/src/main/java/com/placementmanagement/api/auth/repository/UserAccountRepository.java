package com.placementmanagement.api.auth.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.placementmanagement.api.auth.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> findByIdForUpdate(@Param("id") Long id);

    Optional<UserAccount> findByStudentEnrollmentNumber(String enrollmentNumber);
    Optional<UserAccount> findByUniversityEmailIgnoreCase(String universityEmail);
    boolean existsByStudentEnrollmentNumber(String enrollmentNumber);
    boolean existsByUniversityEmail(String universityEmail);
    boolean existsByUniversityEmailIgnoreCase(String universityEmail);
    List<UserAccount> findAllByRole_RoleCodeOrderByFullName(String roleCode);
}
