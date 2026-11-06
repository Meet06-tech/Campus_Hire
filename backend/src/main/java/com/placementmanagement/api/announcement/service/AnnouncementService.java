package com.placementmanagement.api.announcement.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.announcement.dto.AnnouncementDtos.AnnouncementRequest;
import com.placementmanagement.api.announcement.dto.AnnouncementDtos.AnnouncementResponse;
import com.placementmanagement.api.announcement.entity.Announcement;
import com.placementmanagement.api.announcement.repository.AnnouncementRepository;
import com.placementmanagement.api.announcement.repository.AnnouncementRepository.AnnouncementProjection;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;

@Service
public class AnnouncementService {
    private static final int MAX_MESSAGE_LENGTH = 10000;

    private final AnnouncementRepository announcements;
    private final PlacementAuthorizationService authorization;

    public AnnouncementService(AnnouncementRepository announcements, PlacementAuthorizationService authorization) {
        this.announcements = announcements;
        this.authorization = authorization;
    }

    @Transactional
    public AnnouncementResponse publish(Authentication authentication, AnnouncementRequest request) {
        UserAccount publisher = authorization.staff(authentication);
        if (request == null) throw bad("Announcement details are required");
        String subject = required(request.subject(), "Subject");
        String title = required(request.title(), "Title");
        String message = required(request.message(), "Message");
        if (subject.length() > 200) throw bad("Subject cannot exceed 200 characters");
        if (title.length() > 200) throw bad("Title cannot exceed 200 characters");
        if (message.length() > MAX_MESSAGE_LENGTH) throw bad("Message cannot exceed 10000 characters");

        Department audience = null;
        if (publisher.getRole() == UserRole.DTPO) {
            audience = publisher.getDepartment();
            if (audience == null) throw bad("DTPO department is not configured");
            if (!audience.isActive()) throw bad("DTPO department is inactive");
        }
        return response(announcements.save(new Announcement(subject, title, message, publisher, audience)));
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> forStudent(Authentication authentication) {
        var student = authorization.student(authentication);
        if (student.getDepartment() == null) throw bad("Student department is not configured");
        return announcements.findVisibleToStudent(student.getDepartment().getId()).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> forStaff(Authentication authentication) {
        UserAccount staff = authorization.staff(authentication);
        if (staff.getRole() == UserRole.UTPO) return announcements.findAllForStaff().stream().map(this::response).toList();
        if (staff.getDepartment() == null) throw bad("DTPO department is not configured");
        return announcements.findForDepartment(staff.getDepartment().getId()).stream().map(this::response).toList();
    }

    private AnnouncementResponse response(Announcement announcement) {
        return new AnnouncementResponse(announcement.getId(), announcement.getSubject(), announcement.getTitle(),
                announcement.getMessage(), announcement.getPublisher().getFullName(), announcement.getPublisher().getRole(),
                announcement.getDepartment() == null ? null : announcement.getDepartment().getId(),
                announcement.getDepartment() == null ? null : announcement.getDepartment().getName(), announcement.getPublishedAt());
    }

    private AnnouncementResponse response(AnnouncementProjection announcement) {
        return new AnnouncementResponse(announcement.getId(), announcement.getSubject(), announcement.getTitle(),
                announcement.getMessage(), announcement.getPublisherName(), UserRole.valueOf(announcement.getPublisherRole()),
                announcement.getDepartmentId(), announcement.getDepartmentName(), announcement.getPublishedAt());
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) throw bad(field + " is required");
        return value.trim();
    }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
