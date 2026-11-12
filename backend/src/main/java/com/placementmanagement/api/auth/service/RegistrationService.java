package com.placementmanagement.api.auth.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.AuthDtos.RegisterRequest;
import com.placementmanagement.api.auth.dto.RegistrationDtos.RegistrationCompleteResponse;
import com.placementmanagement.api.auth.dto.RegistrationDtos.RegistrationOtpRequest;
import com.placementmanagement.api.auth.dto.RegistrationDtos.RegistrationStartResponse;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.DepartmentEmailCode;
import com.placementmanagement.api.auth.entity.PendingStudentRegistration;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.DepartmentEmailCodeRepository;
import com.placementmanagement.api.auth.repository.PendingStudentRegistrationRepository;
import com.placementmanagement.api.auth.repository.RoleRepository;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;

@Service
public class RegistrationService {
    private static final long OTP_LIFETIME_SECONDS = 300;
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private final SecureRandom random = new SecureRandom();
    private final UserAccountRepository users;
    private final StudentRepository students;
    private final DepartmentEmailCodeRepository departmentEmailCodes;
    private final PendingStudentRegistrationRepository registrations;
    private final UniversityEmailParser emailParser;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationOtpAttemptService otpAttempts;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String supportFrom;

    public RegistrationService(UserAccountRepository users, StudentRepository students,
            DepartmentEmailCodeRepository departmentEmailCodes,
            PendingStudentRegistrationRepository registrations,
            UniversityEmailParser emailParser, RoleRepository roles,
            PasswordEncoder passwordEncoder, RegistrationOtpAttemptService otpAttempts,
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${app.support.from-email}") String supportFrom) {
        this.users = users;
        this.students = students;
        this.departmentEmailCodes = departmentEmailCodes;
        this.registrations = registrations;
        this.emailParser = emailParser;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.otpAttempts = otpAttempts;
        this.mailSenders = mailSenders;
        this.supportFrom = supportFrom;
    }

    @Transactional
    public RegistrationStartResponse begin(RegisterRequest request) {
        String enrollment = required(request == null ? null : request.enrollmentNumber(), "Enrollment number");
        String email = required(request == null ? null : request.universityEmail(), "University email")
                .toLowerCase(Locale.ROOT);
        String name = required(request == null ? null : request.fullName(), "Name");
        String password = required(request == null ? null : request.password(), "Password");
        if (password.length() < 8) throw bad("Password must be at least 8 characters");
        if (request.confirmPassword() != null && !password.equals(request.confirmPassword())) {
            throw bad("Passwords do not match");
        }
        if (users.existsByStudentEnrollmentNumber(enrollment)
                || registrations.existsByEnrollmentNumber(enrollment)) {
            throw conflict("Enrollment number is already registered or pending verification");
        }
        if (users.existsByUniversityEmailIgnoreCase(email)
                || registrations.existsByUniversityEmail(email)) {
            throw conflict("University email is already registered or pending verification");
        }
        String emailCode = emailParser.extractCode(email);
        Department department = departmentEmailCodes.findByEmailCodeAndActiveTrue(emailCode)
                .map(DepartmentEmailCode::getDepartment)
                .filter(Department::isActive)
                .orElseThrow(() -> bad("University email code is not recognized or its department is inactive"));

        Instant now = Instant.now();
        String otp = String.format("%06d", random.nextInt(1_000_000));
        PendingStudentRegistration pending = registrations.saveAndFlush(new PendingStudentRegistration(
                name, enrollment, email, passwordEncoder.encode(password), department,
                passwordEncoder.encode(otp), now.plus(OTP_LIFETIME_SECONDS, ChronoUnit.SECONDS)));
        sendOtp(email, otp);
        return new RegistrationStartResponse(pending.getId(), mask(email), OTP_LIFETIME_SECONDS);
    }

    @Transactional
    public RegistrationCompleteResponse verify(RegistrationOtpRequest request) {
        if (request == null || request.registrationId() == null
                || request.otp() == null || !request.otp().matches("\\d{6}")) {
            throw bad("Registration ID and a 6-digit OTP are required");
        }
        RegistrationOtpAttemptService.Result result = otpAttempts.verify(request.registrationId(), request.otp());
        switch (result) {
            case INVALID -> throw bad("The registration OTP is invalid");
            case LOCKED -> throw bad("Too many OTP attempts. Request a new registration");
            case EXPIRED -> throw bad("The registration OTP has expired");
            case ALREADY_VERIFIED -> throw bad("This registration OTP has already been verified");
            case NOT_FOUND -> throw notFound("Registration verification was not found");
            case VERIFIED -> { }
        }

        PendingStudentRegistration pending = registrations.findByIdForUpdate(request.registrationId())
                .orElseThrow(() -> notFound("Registration verification was not found"));
        if (pending.getConsumedAt() != null) throw conflict("Registration has already been completed");
        if (pending.getExpiresAt().isBefore(Instant.now())) throw bad("The registration OTP has expired");
        if (users.existsByStudentEnrollmentNumber(pending.getEnrollmentNumber())
                || users.existsByUniversityEmailIgnoreCase(pending.getUniversityEmail())) {
            throw conflict("This registration is already completed");
        }
        UserAccount user = new UserAccount(pending.getFullName(), pending.getUniversityEmail(), pending.getPasswordHash());
        user.setRole(roles.findByRoleCode(UserRole.STUDENT.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Student role is not configured")));
        user = users.saveAndFlush(user);
        students.save(new Student(user, pending.getEnrollmentNumber(), pending.getDepartment()));
        pending.markConsumed();
        return new RegistrationCompleteResponse("Registration completed. You can now sign in.");
    }

    @Transactional
    public RegistrationStartResponse resend(Long registrationId) {
        PendingStudentRegistration pending = registrations.findByIdForUpdate(registrationId)
                .orElseThrow(() -> notFound("Registration verification was not found"));
        if (pending.getConsumedAt() != null) throw conflict("Registration has already been completed");
        if (pending.getVerifiedAt() != null) throw conflict("Registration OTP has already been verified");
        Instant now = Instant.now();
        if (pending.getRequestedAt().plus(RESEND_COOLDOWN_SECONDS, ChronoUnit.SECONDS).isAfter(now)) {
            throw bad("Please wait before requesting another OTP");
        }
        String otp = String.format("%06d", random.nextInt(1_000_000));
        pending.replaceOtp(passwordEncoder.encode(otp), now, now.plus(OTP_LIFETIME_SECONDS, ChronoUnit.SECONDS));
        sendOtp(pending.getUniversityEmail(), otp);
        return new RegistrationStartResponse(pending.getId(), mask(pending.getUniversityEmail()), OTP_LIFETIME_SECONDS);
    }

    private void sendOtp(String email, String otp) {
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null) throw unavailable("Registration email service is not configured");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(supportFrom);
        message.setSubject("CampusHire registration OTP");
        message.setText("Your CampusHire registration OTP is " + otp + ". It expires in 5 minutes.");
        try { sender.send(message); }
        catch (RuntimeException exception) {
            throw unavailable("The registration OTP could not be delivered. Please try again later");
        }
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        return local.substring(0, 1) + "•••@" + email.substring(at + 1);
    }
    private String required(String value, String field) {
        if (value == null || value.isBlank()) throw bad(field + " is required");
        return value.trim();
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private ResponseStatusException unavailable(String message) { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message); }
}
