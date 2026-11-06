package com.placementmanagement.api.announcement.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.placementmanagement.api.announcement.entity.Announcement;
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    interface AnnouncementProjection {
        Long getId();
        String getSubject();
        String getTitle();
        String getMessage();
        String getPublisherName();
        String getPublisherRole();
        Long getDepartmentId();
        String getDepartmentName();
        Instant getPublishedAt();
    }

    @Query("""
        select a.id as id, a.subject as subject, a.title as title, a.message as message,
               a.publisher.fullName as publisherName, a.publisher.role.roleCode as publisherRole,
               d.id as departmentId, d.name as departmentName, a.publishedAt as publishedAt
        from Announcement a join a.publisher left join a.department d
        order by a.publishedAt desc, a.id desc
        """)
    List<AnnouncementProjection> findAllForStaff();

    @Query("""
        select a.id as id, a.subject as subject, a.title as title, a.message as message,
               a.publisher.fullName as publisherName, a.publisher.role.roleCode as publisherRole,
               d.id as departmentId, d.name as departmentName, a.publishedAt as publishedAt
        from Announcement a join a.publisher left join a.department d
        where a.department is null or a.department.id = :departmentId
        order by a.publishedAt desc, a.id desc
        """)
    List<AnnouncementProjection> findVisibleToStudent(@Param("departmentId") Long departmentId);

    @Query("""
        select a.id as id, a.subject as subject, a.title as title, a.message as message,
               a.publisher.fullName as publisherName, a.publisher.role.roleCode as publisherRole,
               d.id as departmentId, d.name as departmentName, a.publishedAt as publishedAt
        from Announcement a join a.publisher join a.department d
        where a.department.id = :departmentId
        order by a.publishedAt desc, a.id desc
        """)
    List<AnnouncementProjection> findForDepartment(@Param("departmentId") Long departmentId);
}
