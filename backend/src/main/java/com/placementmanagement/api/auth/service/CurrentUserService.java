package com.placementmanagement.api.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.MeDtos.DepartmentResponse;
import com.placementmanagement.api.auth.dto.MeDtos.MeResponse;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.repository.UserAccountRepository;

@Service
public class CurrentUserService {
    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public MeResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        Long userId;
        try {
            userId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication subject");
        }

        UserAccount user = users.findById(userId)
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is invalid"));
        Student student = user.getStudent();
        DepartmentResponse department = student == null ? null : department(student.getDepartment());

        return new MeResponse(
                user.getId(),
                user.getFullName(),
                user.getUniversityEmail(),
                student == null ? null : student.getEnrollmentNumber(),
                user.getRole().name(),
                user.isEnabled(),
                department);
    }

    private DepartmentResponse department(Department department) {
        return department == null ? null
                : new DepartmentResponse(department.getId(), department.getCode(), department.getName());
    }
}
