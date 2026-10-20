package com.placementmanagement.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.AuthDtos.AuthResponse;
import com.placementmanagement.api.auth.AuthDtos.LoginRequest;
import com.placementmanagement.api.auth.AuthDtos.RegisterRequest;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository users, RoleRepository roles, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String enrollment = required(request.enrollmentNumber(), "Enrollment number");
        String email = required(request.universityEmail(), "University email").toLowerCase();
        String name = required(request.fullName(), "Name");
        String password = required(request.password(), "Password");
        if (password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        if (users.existsByEnrollmentNumber(enrollment)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Enrollment number is already registered");
        }
        if (users.existsByUniversityEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "University email is already registered");
        }
        UserAccount user = new UserAccount(name, enrollment, email, passwordEncoder.encode(password));
        user.setRole(roles.findByRoleCode(UserRole.STUDENT.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Student role is not configured")));
        user = users.save(user);
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByEnrollmentNumber(required(request.enrollmentNumber(), "Enrollment number"))
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(required(request.password(), "Password"), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return response(user);
    }

    private AuthResponse response(UserAccount user) {
        return new AuthResponse(jwtService.createToken(user), user.getId().toString(), user.getFullName(),
                user.getEnrollmentNumber(), user.getRole().name());
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }
}

