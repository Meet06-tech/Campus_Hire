package com.placementmanagement.api.announcement.dto;

import java.time.Instant;

import com.placementmanagement.api.auth.entity.UserRole;

public final class AnnouncementDtos {
    private AnnouncementDtos() {}

    public record AnnouncementRequest(String subject, String title, String message) {}

    public record AnnouncementResponse(Long id, String subject, String title, String message,
            String publisherName, UserRole publisherRole, Long departmentId, String departmentName,
            Instant publishedAt) {}
}
