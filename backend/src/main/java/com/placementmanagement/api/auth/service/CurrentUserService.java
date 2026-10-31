package com.placementmanagement.api.auth.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.MeDtos.DepartmentResponse;
import com.placementmanagement.api.auth.dto.MeDtos.MeResponse;
import com.placementmanagement.api.auth.dto.MeDtos.ProfileResponse;
import com.placementmanagement.api.auth.dto.MeDtos.ProfileUpdateRequest;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.placement.entity.PlacementProgram;
import com.placementmanagement.api.placement.repository.PlacementProgramRepository;

@Service
public class CurrentUserService {
    private final UserAccountRepository users;
    private final StudentRepository students;
    private final PlacementProgramRepository programs;

    public CurrentUserService(UserAccountRepository users, StudentRepository students) {
        this(users, students, null);
    }

    @Autowired
    public CurrentUserService(UserAccountRepository users, StudentRepository students,
            PlacementProgramRepository programs) {
        this.users = users;
        this.students = students;
        this.programs = programs;
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
        DepartmentResponse department = user.getRole() == UserRole.STUDENT
                ? (student == null ? null : department(student.getDepartment()))
                : department(user.getDepartment());

        return new MeResponse(
                user.getId(),
                user.getFullName(),
                user.getUniversityEmail(),
                student == null ? null : student.getEnrollmentNumber(),
                user.getRole().name(),
                user.isEnabled(),
                department,
                profile(student),
                photoUrl(student));
    }

    @Transactional
    public MeResponse updateCurrentUser(Authentication authentication, ProfileUpdateRequest request) {
        UserAccount user = authenticatedUser(authentication);
        if (user.getRole() != UserRole.STUDENT || user.getStudent() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only student profiles can be updated");
        }
        validate(request, user.getStudent().getAcademicProgram(), user.getStudent().getCurrentSemester());
        if (request.fullName() != null) {
            user.updateFullName(required(request.fullName(), "Full name"));
        }
        Student student = user.getStudent();
        student.updateProfile(
                request.admissionYear() == null ? student.getAdmissionYear()
                        : toYear(request.admissionYear(), "Admission year"),
                request.graduationYear() == null ? student.getGraduationYear()
                        : toYear(request.graduationYear(), "Graduation year"),
                request.dateOfBirth() == null ? student.getDateOfBirth() : request.dateOfBirth(),
                request.phoneNumber() == null ? student.getPhoneNumber()
                        : bounded(request.phoneNumber(), "Phone number", 50),
                request.academicProgram() == null ? student.getAcademicProgram()
                        : bounded(request.academicProgram(), "Academic program", 200),
                request.currentSemester() == null ? student.getCurrentSemester()
                        : toSemester(request.currentSemester()),
                request.cgpa() == null ? student.getCgpa() : request.cgpa(),
                request.careerProfile() == null ? student.getCareerProfile() : request.careerProfile(),
                request.skills() == null ? null : normalizeSkills(request.skills()));
        users.save(user);
        students.save(student);
        return currentUser(authentication);
    }

    private DepartmentResponse department(Department department) {
        return department == null ? null
                : new DepartmentResponse(department.getId(), department.getCode(), department.getName());
    }

    private ProfileResponse profile(Student student) {
        return student == null ? null : new ProfileResponse(
                student.getAdmissionYear(), student.getGraduationYear(), student.getDateOfBirth(),
                student.getPhoneNumber(), student.getAcademicProgram(), student.getCurrentSemester(),
                student.getCgpa(), student.getCareerProfile(), List.copyOf(student.getSkills()),
                programDuration(student));
    }

    private String photoUrl(Student student) {
        return student == null || student.getProfilePhotoKey() == null
                ? null : "/api/me/profile-photo";
    }

    @Transactional(readOnly = true)
    public Student authenticatedStudent(Authentication authentication) {
        UserAccount user = authenticatedUser(authentication);
        if (user.getRole() != UserRole.STUDENT || user.getStudent() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only student profiles can be changed");
        }
        return user.getStudent();
    }

    private UserAccount authenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        try {
            return users.findById(Long.valueOf(authentication.getName()))
                    .filter(UserAccount::isEnabled)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is invalid"));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication subject");
        }
    }

    private void validate(ProfileUpdateRequest request, String existingProgram, Short existingSemester) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile update is required");
        }
        if (request.fullName() == null && request.admissionYear() == null && request.graduationYear() == null
                && request.dateOfBirth() == null && request.phoneNumber() == null
                && request.academicProgram() == null && request.currentSemester() == null
                && request.cgpa() == null && request.careerProfile() == null && request.skills() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one editable field is required");
        }
        if (request.dateOfBirth() != null && request.dateOfBirth().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth cannot be in the future");
        }
        if (request.cgpa() != null && (request.cgpa().compareTo(BigDecimal.ZERO) < 0
                || request.cgpa().compareTo(BigDecimal.TEN) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CGPA must be between 0 and 10");
        }
        if (request.currentSemester() != null && request.currentSemester() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semester must be positive");
        }
        boolean programChanged = request.academicProgram() != null
                && !request.academicProgram().trim().equalsIgnoreCase(existingProgram == null ? "" : existingProgram.trim());
        boolean semesterChanged = request.currentSemester() != null
                && (existingSemester == null || !request.currentSemester().equals(existingSemester.intValue()));
        if (programChanged || semesterChanged) {
            Integer semester = request.currentSemester() == null
                    ? existingSemester == null ? null : existingSemester.intValue()
                    : request.currentSemester();
            if (semester != null) {
                validateSemester(request.academicProgram() == null ? existingProgram : request.academicProgram(), semester);
            }
        }
        if (request.admissionYear() != null && !validYear(request.admissionYear())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admission year is invalid");
        }
        if (request.graduationYear() != null && !validYear(request.graduationYear())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Graduation year is invalid");
        }
        bounded(request.phoneNumber(), "Phone number", 50);
        bounded(request.academicProgram(), "Academic program", 200);
        if (request.skills() != null) normalizeSkills(request.skills());
    }

    private boolean validYear(Integer year) { return year >= 1900 && year <= 2200; }

    private Short toYear(Integer year, String field) {
        return year == null ? null : year.shortValue();
    }

    private Short toSemester(Integer semester) {
        return semester == null ? null : semester.shortValue();
    }

    private void validateSemester(String requestedProgram, Integer semester) {
        if (requestedProgram == null || requestedProgram.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Academic program is required to validate semester duration");
        }
        if (programs == null) return;
        PlacementProgram program = programs.findByCodeIgnoreCase(requestedProgram.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Program duration is not configured for this academic program"));
        if (program.getDurationSemesters() == null || semester > program.getDurationSemesters()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Semester must be within your program duration");
        }
    }

    private Set<String> normalizeSkills(List<String> values) {
        if (values.size() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can select at most 30 skills");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank() || value.trim().length() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Skills must be non-empty and 100 characters or fewer");
            }
            normalized.add(value.trim());
        }
        return normalized;
    }

    private Integer programDuration(Student student) {
        if (programs == null || student == null || student.getAcademicProgram() == null) return null;
        return programs.findByCodeIgnoreCase(student.getAcademicProgram().trim())
                .map(PlacementProgram::getDurationSemesters)
                .map(Short::intValue)
                .orElse(null);
    }

    private String required(String value, String field) {
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " cannot be blank");
        }
        return value.trim();
    }

    private String bounded(String value, String field, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is too long");
        }
        return value;
    }
}
