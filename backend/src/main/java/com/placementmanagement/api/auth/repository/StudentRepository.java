package com.placementmanagement.api.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.placement.dto.PlacementDtos.StaffStudentProjection;

public interface StudentRepository extends JpaRepository<Student, Long> {
    long countByDepartment_Id(Long departmentId);

    @Query("""
        select s.id as studentId, u.fullName as fullName, u.universityEmail as universityEmail,
               s.enrollmentNumber as enrollmentNumber, d.id as departmentId, d.code as departmentCode,
               d.name as departmentName, s.academicProgram as academicProgram,
               s.graduationYear as graduationYear, s.cgpa as cgpa,
               count(a.id) as applicationCount
        from Student s join s.user u join s.department d
        left join com.placementmanagement.api.placement.entity.PlacementApplication a on a.student = s
        where (:departmentId is null or d.id = :departmentId)
          and (cast(:search as String) is null or lower(u.fullName) like lower(concat('%', cast(:search as String), '%'))
               or lower(s.enrollmentNumber) like lower(concat('%', cast(:search as String), '%'))
               or lower(coalesce(s.academicProgram, '')) like lower(concat('%', cast(:search as String), '%')))
        group by s.id, u.fullName, u.universityEmail, s.enrollmentNumber,
                 d.id, d.code, d.name, s.academicProgram, s.graduationYear, s.cgpa
        order by u.fullName
        """)
    Page<StaffStudentProjection> findStaffStudents(@Param("departmentId") Long departmentId,
            @Param("search") String search, Pageable pageable);
}
