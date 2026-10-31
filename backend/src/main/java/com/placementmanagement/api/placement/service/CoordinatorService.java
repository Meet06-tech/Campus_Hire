package com.placementmanagement.api.placement.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.AuthDtos.DepartmentInfo;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Role;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.DepartmentRepository;
import com.placementmanagement.api.auth.repository.RoleRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.service.StaffEmailAllowlist;
import com.placementmanagement.api.placement.dto.PlacementDtos.CoordinatorRequest;
import com.placementmanagement.api.placement.dto.PlacementDtos.CoordinatorResponse;

@Service
public class CoordinatorService {
    private final UserAccountRepository users;
    private final DepartmentRepository departments;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final PlacementAuthorizationService authorization;
    private final StaffEmailAllowlist emailAllowlist;

    public CoordinatorService(UserAccountRepository users, DepartmentRepository departments, RoleRepository roles,
            PasswordEncoder passwordEncoder, PlacementAuthorizationService authorization,
            StaffEmailAllowlist emailAllowlist) {
        this.users = users;
        this.departments = departments;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.authorization = authorization;
        this.emailAllowlist = emailAllowlist;
    }

    @Transactional
    public CoordinatorResponse create(Authentication authentication, CoordinatorRequest request) {
        requireUtpo(authentication);
        if (request == null) throw bad("Coordinator details are required");
        String name = required(request.fullName(), "Full name");
        String email = required(request.universityEmail(), "University email").toLowerCase(Locale.ROOT);
        String password = required(request.initialPassword(), "Initial password");
        if (!emailAllowlist.accepts(email)) throw bad("Email domain is not allowed for staff accounts");
        if (password.length() < 8) throw bad("Initial password must be at least 8 characters");
        if (request.departmentId() == null) throw bad("Department is required for a DTPO");
        if (users.existsByUniversityEmailIgnoreCase(email)) throw conflict("University email is already registered");
        Department department = departments.findById(request.departmentId())
                .orElseThrow(() -> notFound("Department"));
        if (!department.isActive()) throw bad("Department is inactive");
        Role role = roles.findByRoleCode(UserRole.DTPO.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DTPO role is not configured"));

        UserAccount user = new UserAccount(name, email, passwordEncoder.encode(password));
        user.setRole(role);
        user.setDepartment(department);
        return response(users.save(user));
    }

    @Transactional(readOnly = true)
    public List<CoordinatorResponse> list(Authentication authentication) {
        requireUtpo(authentication);
        return users.findAllByRole_RoleCodeOrderByFullName(UserRole.DTPO.name()).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentInfo> departments(Authentication authentication) {
        authorization.staff(authentication);
        return departments.findAllByActiveTrueOrderByNameAsc().stream()
                .map(department -> new DepartmentInfo(department.getId(), department.getCode(), department.getName()))
                .toList();
    }

    private UserAccount requireUtpo(Authentication authentication) {
        UserAccount user = authorization.staff(authentication);
        if (user.getRole() != UserRole.UTPO) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "UTPO access is required");
        return user;
    }

    private CoordinatorResponse response(UserAccount user) {
        Department department = user.getDepartment();
        return new CoordinatorResponse(user.getId(), user.getFullName(), user.getUniversityEmail(),
                department == null ? null : new DepartmentInfo(department.getId(), department.getCode(), department.getName()),
                user.isEnabled());
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) throw bad(field + " is required");
        return value.trim();
    }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private ResponseStatusException notFound(String type) { return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found"); }
}
