package com.placementmanagement.api.student.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.placementmanagement.api.student.entity.PasswordChangeOtp;

public interface PasswordChangeOtpRepository extends JpaRepository<PasswordChangeOtp, Long> {
    Optional<PasswordChangeOtp> findTopByUser_IdAndUsedAtIsNullOrderByRequestedAtDesc(Long userId);
    Optional<PasswordChangeOtp> findTopByUser_IdOrderByRequestedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PasswordChangeOtp o where o.user.id = :userId and o.usedAt is null order by o.requestedAt desc")
    List<PasswordChangeOtp> findActiveForUpdate(@Param("userId") Long userId);

    List<PasswordChangeOtp> findAllByUser_IdAndUsedAtIsNull(Long userId);
}
