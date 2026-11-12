package com.placementmanagement.api.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.placementmanagement.api.auth.entity.PendingStudentRegistration;

public interface PendingStudentRegistrationRepository extends JpaRepository<PendingStudentRegistration, Long> {
    boolean existsByEnrollmentNumber(String enrollmentNumber);
    boolean existsByUniversityEmail(String universityEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PendingStudentRegistration r where r.id = :id")
    Optional<PendingStudentRegistration> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PendingStudentRegistration r where r.id = :id")
    Optional<PendingStudentRegistration> findByIdForUpdateForVerification(@Param("id") Long id);
}
