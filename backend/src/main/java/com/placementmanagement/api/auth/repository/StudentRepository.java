package com.placementmanagement.api.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placementmanagement.api.auth.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {
}
