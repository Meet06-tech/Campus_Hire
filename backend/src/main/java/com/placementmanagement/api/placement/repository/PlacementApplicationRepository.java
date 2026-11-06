package com.placementmanagement.api.placement.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.placementmanagement.api.placement.entity.PlacementApplication;
import com.placementmanagement.api.placement.entity.ApplicationStatus;
import com.placementmanagement.api.placement.entity.OfferStatus;
import com.placementmanagement.api.placement.dto.PlacementDtos.StaffApplicationProjection;
public interface PlacementApplicationRepository extends JpaRepository<PlacementApplication, Long> {
    boolean existsByDrive_IdAndStudent_Id(Long driveId, Long studentId);
    boolean existsByStudent_IdAndOfferStatus(Long studentId, OfferStatus offerStatus);
    List<PlacementApplication> findAllByStudent_IdOrderByAppliedAtDesc(Long studentId);
    List<PlacementApplication> findAllByDrive_IdOrderByAppliedAtAsc(Long driveId);
    List<PlacementApplication> findAllByStatusOrderByAppliedAtAsc(ApplicationStatus status);
    List<PlacementApplication> findAllByStatusAndStudent_Department_IdOrderByAppliedAtAsc(ApplicationStatus status, Long departmentId);
    long countByStatus(ApplicationStatus status);
    long countByDrive_Department_Id(Long departmentId);
    long countByDrive_Department_IdAndStatus(Long departmentId, ApplicationStatus status);

    @Query("select distinct a.drive.id from PlacementApplication a where a.student.department.id = :departmentId")
    Set<Long> findDistinctDriveIdsByStudentDepartmentId(@Param("departmentId") Long departmentId);

    interface CompanyHiringProjection {
        Long getCompanyId();
        String getCompanyName();
        String getDepartmentName();
        String getDriveTitle();
        java.math.BigDecimal getCtcAmount();
        String getCtcCurrency();
        java.time.Instant getPlacementDate();
        Long getHiredCount();
    }

    @Query("""
        select c.id as companyId, c.name as companyName, s.department.name as departmentName, drive.title as driveTitle,
               drive.ctcAmount as ctcAmount, drive.ctcCurrency as ctcCurrency,
               min(a.placementSelectedAt) as placementDate, count(distinct a.id) as hiredCount
        from PlacementApplication a
        join a.drive drive join drive.company c
        join a.student s
        where a.status = :status
        group by c.id, c.name, s.department.name, drive.title, drive.ctcAmount, drive.ctcCurrency, drive.id
        order by c.name, s.department.name, drive.title
        """)
    List<CompanyHiringProjection> findCompanyHiringReport(@Param("status") ApplicationStatus status);

    @Query("""
        select c.id as companyId, c.name as companyName, s.department.name as departmentName, drive.title as driveTitle,
               drive.ctcAmount as ctcAmount, drive.ctcCurrency as ctcCurrency,
               min(a.placementSelectedAt) as placementDate, count(distinct a.id) as hiredCount
        from PlacementApplication a
        join a.drive drive join drive.company c
        join a.student s
        where a.status = :status and s.department.id = :departmentId
        group by c.id, c.name, s.department.name, drive.title, drive.ctcAmount, drive.ctcCurrency, drive.id
        order by c.name, s.department.name, drive.title
        """)
    List<CompanyHiringProjection> findCompanyHiringReportByStudentDepartment(
            @Param("status") ApplicationStatus status, @Param("departmentId") Long departmentId);

    @Query("""
        select c.id as companyId, c.name as companyName, s.department.name as departmentName, drive.title as driveTitle,
               drive.ctcAmount as ctcAmount, drive.ctcCurrency as ctcCurrency,
               min(a.placementSelectedAt) as placementDate, count(distinct a.id) as hiredCount
        from PlacementApplication a
        join a.drive drive join drive.company c
        join a.student s
        where a.status = :status and drive.department.id = :departmentId
        group by c.id, c.name, s.department.name, drive.title, drive.ctcAmount, drive.ctcCurrency, drive.id
        order by c.name, s.department.name, drive.title
        """)
    List<CompanyHiringProjection> findCompanyHiringReportByDriveDepartment(
            @Param("status") ApplicationStatus status, @Param("departmentId") Long departmentId);

    @Query("select count(distinct a.student.id) from PlacementApplication a where (:departmentId is null or a.drive.department.id = :departmentId)")
    long countDistinctStudentsByDriveDepartment(@Param("departmentId") Long departmentId);

    @Query("""
        select a.id as applicationId, s.id as studentId, u.fullName as studentName,
               u.universityEmail as universityEmail, s.enrollmentNumber as enrollmentNumber,
               d.id as departmentId, d.code as departmentCode, d.name as departmentName,
               s.academicProgram as academicProgram, s.cgpa as studentCgpa, s.graduationYear as studentGraduationYear,
               drive.id as driveId, drive.title as driveTitle,
               c.name as companyName, a.appliedAt as appliedAt, a.status as status
        from PlacementApplication a
        join a.student s join s.user u join s.department d
        join a.drive drive join drive.company c
        where (:departmentId is null or drive.department.id = :departmentId)
          and (:status is null or a.status = :status)
          and (:companyId is null or c.id = :companyId)
          and (:minCgpa is null or s.cgpa >= :minCgpa)
          and (:maxCgpa is null or s.cgpa <= :maxCgpa)
          and (cast(:search as String) is null or lower(u.fullName) like lower(concat('%', cast(:search as String), '%'))
               or lower(s.enrollmentNumber) like lower(concat('%', cast(:search as String), '%'))
               or lower(drive.title) like lower(concat('%', cast(:search as String), '%'))
               or lower(c.name) like lower(concat('%', cast(:search as String), '%'))
               or lower(u.universityEmail) like lower(concat('%', cast(:search as String), '%')))
        order by a.appliedAt desc
        """)
    Page<StaffApplicationProjection> findStaffApplications(@Param("departmentId") Long departmentId,
            @Param("status") ApplicationStatus status, @Param("companyId") Long companyId,
            @Param("minCgpa") java.math.BigDecimal minCgpa, @Param("maxCgpa") java.math.BigDecimal maxCgpa,
            @Param("search") String search, Pageable pageable);
}
