package com.placementmanagement.api.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.placementmanagement.api.auth.dto.MeDtos.MeResponse;
import com.placementmanagement.api.auth.dto.MeDtos.ProfileUpdateRequest;
import com.placementmanagement.api.auth.service.CurrentUserService;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final CurrentUserService currentUserService;

    public MeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public MeResponse me(Authentication authentication) {
        return currentUserService.currentUser(authentication);
    }

    @PutMapping
    public MeResponse update(@RequestBody ProfileUpdateRequest request, Authentication authentication) {
        return currentUserService.updateCurrentUser(authentication, request);
    }
}
