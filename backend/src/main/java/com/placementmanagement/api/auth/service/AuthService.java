package com.placementmanagement.api.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.AuthDtos.AuthResponse;
import com.placementmanagement.api.auth.dto.AuthDtos.LoginRequest;
import com.placementmanagement.api.auth.dto.AuthDtos.RegisterRequest;
import com.placementmanagement.api.auth.dto.AuthDtos.DepartmentInfo;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.DepartmentEmailCode;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.DepartmentEmailCodeRepository;
import com.placementmanagement.api.auth.repository.RoleRepository;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.security.JwtService;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final StudentRepository students;
    private final DepartmentEmailCodeRepository departmentEmailCodes;
    private final UniversityEmailParser emailParser;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository users, StudentRepository students,
            DepartmentEmailCodeRepository departmentEmailCodes,
            UniversityEmailParser emailParser, RoleRepository roles,
            PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.students = students;
        this.departmentEmailCodes = departmentEmailCodes;
        this.emailParser = emailParser;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String enrollment = required(request.enrollmentNumber(), "Enrollment number");
        String email = required(request.universityEmail(), "University email").toLowerCase();
        String name = required(request.fullName(), "Name");
        String password = required(request.password(), "Password");
        if (password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        if (request.confirmPassword() != null && !password.equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (users.existsByStudentEnrollmentNumber(enrollment)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Enrollment number is already registered");
        }
        if (users.existsByUniversityEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "University email is already registered");
        }
        String emailCode = emailParser.extractCode(email);
        Department department = departmentEmailCodes.findByEmailCodeAndActiveTrue(emailCode)
                .map(DepartmentEmailCode::getDepartment)
                .filter(Department::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "University email code is not recognized or its department is inactive"));
        UserAccount user = new UserAccount(name, email, passwordEncoder.encode(password));
        user.setRole(roles.findByRoleCode(UserRole.STUDENT.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Student role is not configured")));
        user = users.save(user);
        students.save(new Student(user, enrollment, department));
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Login request is required");
        }
        boolean hasIdentifier = request.identifier() != null && !request.identifier().isBlank();
        boolean hasEnrollment = request.enrollmentNumber() != null && !request.enrollmentNumber().isBlank();
        if (hasIdentifier == hasEnrollment) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide exactly one of identifier or enrollmentNumber");
        }
        String password = required(request.password(), "Password");
        UserAccount user = (hasIdentifier
                ? users.findByUniversityEmailIgnoreCase(required(request.identifier(), "Identifier"))
                : users.findByStudentEnrollmentNumber(required(request.enrollmentNumber(), "Enrollment number")))
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return response(user);
    }

    private AuthResponse response(UserAccount user) {
        Department department = user.getDepartment();
        return new AuthResponse(jwtService.createToken(user), user.getId().toString(), user.getFullName(),
                user.getStudent() == null ? null : user.getStudent().getEnrollmentNumber(), user.getRole().name(),
                department == null ? null : new DepartmentInfo(department.getId(), department.getCode(), department.getName()));
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }
}

