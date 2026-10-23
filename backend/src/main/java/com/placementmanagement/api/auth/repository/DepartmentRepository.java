package com.placementmanagement.api.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placementmanagement.api.auth.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByActiveTrueOrderByNameAsc();
}
