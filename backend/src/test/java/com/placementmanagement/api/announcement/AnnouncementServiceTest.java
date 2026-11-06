package com.placementmanagement.api.announcement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.announcement.dto.AnnouncementDtos.AnnouncementRequest;
import com.placementmanagement.api.announcement.entity.Announcement;
import com.placementmanagement.api.announcement.repository.AnnouncementRepository;
import com.placementmanagement.api.announcement.service.AnnouncementService;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {
    @Mock AnnouncementRepository announcements;
    @Mock PlacementAuthorizationService authorization;
    @Mock Authentication authentication;
    @Mock UserAccount publisher;
    @Mock Department department;
    @Mock Student student;

    @Test
    void utpoPublishesUniversityWideAnnouncement() {
        when(authorization.staff(authentication)).thenReturn(publisher);
        when(publisher.getRole()).thenReturn(UserRole.UTPO);
        when(publisher.getFullName()).thenReturn("University TPO");
        when(announcements.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new AnnouncementService(announcements, authorization).publish(authentication,
                new AnnouncementRequest("Placement", "Drive update", "Applications open tomorrow"));

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcements).save(captor.capture());
        assertNull(captor.getValue().getDepartment());
        assertEquals("Drive update", response.title());
    }

    @Test
    void dtpoAudienceIsAlwaysTheirAssignedDepartment() {
        when(authorization.staff(authentication)).thenReturn(publisher);
        when(publisher.getRole()).thenReturn(UserRole.DTPO);
        when(publisher.getDepartment()).thenReturn(department);
        when(department.isActive()).thenReturn(true);
        when(publisher.getFullName()).thenReturn("Department TPO");
        when(announcements.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new AnnouncementService(announcements, authorization).publish(authentication,
                new AnnouncementRequest("Subject", "Title", "Message"));

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcements).save(captor.capture());
        assertEquals(department, captor.getValue().getDepartment());
    }

    @Test
    void rejectsBlankAnnouncementFields() {
        when(authorization.staff(authentication)).thenReturn(publisher);
        assertThrows(ResponseStatusException.class, () -> new AnnouncementService(announcements, authorization)
                .publish(authentication, new AnnouncementRequest(" ", "Title", "Message")));
        verifyNoInteractions(announcements);
    }

    @Test
    void studentRetrievalUsesDepartmentScopedVisibilityQuery() {
        when(authorization.student(authentication)).thenReturn(student);
        when(student.getDepartment()).thenReturn(department);
        when(department.getId()).thenReturn(2L);
        when(announcements.findVisibleToStudent(2L)).thenReturn(List.of());

        assertEquals(List.of(), new AnnouncementService(announcements, authorization).forStudent(authentication));
        verify(announcements).findVisibleToStudent(2L);
    }
}
