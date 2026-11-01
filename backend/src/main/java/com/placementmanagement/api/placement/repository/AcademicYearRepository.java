package com.placementmanagement.api.placement.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.placementmanagement.api.placement.entity.AcademicYear;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    Optional<AcademicYear> findByStatus(String status);
}
