package com.placementmanagement.api.placement.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.placementmanagement.api.auth.entity.*;
import com.placementmanagement.api.auth.repository.UserAccountRepository;

@Service
public class PlacementAuthorizationService {
    private final UserAccountRepository users;
    public PlacementAuthorizationService(UserAccountRepository users) { this.users = users; }
    public UserAccount staff(Authentication authentication) {
        UserAccount user = user(authentication);
        if (user.getRole() != UserRole.DTPO && user.getRole() != UserRole.UTPO)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff access is required");
        return user;
    }
    public Student student(Authentication authentication) {
        UserAccount user = user(authentication);
        if (user.getRole() != UserRole.STUDENT || user.getStudent() == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student access is required");
        return user.getStudent();
    }
    private UserAccount user(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        try { return users.findById(Long.valueOf(authentication.getName())).filter(UserAccount::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is invalid")); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication subject"); }
    }
}
