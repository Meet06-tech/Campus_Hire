package com.placementmanagement.api.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placementmanagement.api.auth.entity.DepartmentEmailCode;

public interface DepartmentEmailCodeRepository extends JpaRepository<DepartmentEmailCode, Long> {
    Optional<DepartmentEmailCode> findByEmailCodeAndActiveTrue(String emailCode);
}
