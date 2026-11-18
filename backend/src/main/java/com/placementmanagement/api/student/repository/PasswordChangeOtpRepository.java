package com.placementmanagement.api.student.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.placementmanagement.api.student.entity.PasswordChangeOtp;
import com.placementmanagement.api.student.entity.PasswordOtpPurpose;

public interface PasswordChangeOtpRepository extends JpaRepository<PasswordChangeOtp, Long> {
    Optional<PasswordChangeOtp> findTopByUser_IdAndUsedAtIsNullOrderByRequestedAtDesc(Long userId);
    Optional<PasswordChangeOtp> findTopByUser_IdOrderByRequestedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PasswordChangeOtp o where o.user.id = :userId and o.purpose = com.placementmanagement.api.student.entity.PasswordOtpPurpose.PASSWORD_CHANGE and o.usedAt is null order by o.requestedAt desc")
    List<PasswordChangeOtp> findActiveForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PasswordChangeOtp o where o.user.id = :userId and o.purpose = :purpose and o.usedAt is null order by o.requestedAt desc")
    List<PasswordChangeOtp> findActiveForUpdate(@Param("userId") Long userId,
            @Param("purpose") PasswordOtpPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PasswordChangeOtp o where o.recoveryTokenHash = :tokenHash and o.purpose = :purpose and o.usedAt is null")
    Optional<PasswordChangeOtp> findRecoveryForUpdate(@Param("tokenHash") String tokenHash,
            @Param("purpose") PasswordOtpPurpose purpose);

    List<PasswordChangeOtp> findAllByUser_IdAndUsedAtIsNull(Long userId);
}
