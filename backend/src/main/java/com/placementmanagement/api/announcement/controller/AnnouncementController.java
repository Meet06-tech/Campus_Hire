package com.placementmanagement.api.announcement.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.placementmanagement.api.announcement.dto.AnnouncementDtos.AnnouncementRequest;
import com.placementmanagement.api.announcement.dto.AnnouncementDtos.AnnouncementResponse;
import com.placementmanagement.api.announcement.service.AnnouncementService;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementService announcements;

    public AnnouncementController(AnnouncementService announcements) { this.announcements = announcements; }

    @GetMapping
    public List<AnnouncementResponse> studentAnnouncements(Authentication authentication) {
        return announcements.forStudent(authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse publish(@RequestBody AnnouncementRequest request, Authentication authentication) {
        return announcements.publish(authentication, request);
    }

    @GetMapping("/staff")
    public List<AnnouncementResponse> staffAnnouncements(Authentication authentication) {
        return announcements.forStaff(authentication);
    }
}
