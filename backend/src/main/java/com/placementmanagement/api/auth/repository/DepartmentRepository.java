package com.placementmanagement.api.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placementmanagement.api.auth.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
