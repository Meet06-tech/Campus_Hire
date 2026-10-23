package com.placementmanagement.api.placement.repository;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import com.placementmanagement.api.placement.entity.*;
public interface PlacementDriveRepository extends JpaRepository<PlacementDrive, Long> {
    List<PlacementDrive> findAllByDepartment_IdOrderByApplicationDeadlineAsc(Long departmentId);
    List<PlacementDrive> findAllByOrderByApplicationDeadlineAsc();
    Optional<PlacementDrive> findByIdAndDepartment_Id(Long id, Long departmentId);
    long countByStatus(DriveStatus status);
    long countByDepartment_Id(Long departmentId);
    long countByDepartment_IdAndStatus(Long departmentId, DriveStatus status);
    @Query("select d from PlacementDrive d where d.status = :status and (d.department is null or d.department.id = :departmentId) order by d.applicationDeadline asc")
    List<PlacementDrive> findVisiblePublished(DriveStatus status, Long departmentId);
}
