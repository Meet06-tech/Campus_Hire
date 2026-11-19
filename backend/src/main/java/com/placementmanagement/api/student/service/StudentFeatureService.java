package com.placementmanagement.api.student.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.ChangePasswordRequest;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.ComplaintRequest;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.DirectoryStudentResponse;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.OtpRequest;
import com.placementmanagement.api.student.entity.PasswordChangeOtp;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;

@Service
public class StudentFeatureService {
    private final SecureRandom random = new SecureRandom();
    private final StudentRepository students;
    private final PlacementAuthorizationService authorization;
    private final PasswordEncoder passwordEncoder;
    private final PasswordOtpAttemptService otpAttempts;
    private final PasswordOtpIssuanceService otpIssuance;
    private final PasswordChangeTransactionService passwordChanges;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String supportRecipient;
    private final String supportFrom;

    public StudentFeatureService(StudentRepository students,
            PlacementAuthorizationService authorization, PasswordEncoder passwordEncoder,
            ObjectProvider<JavaMailSender> mailSenders,
            PasswordOtpAttemptService otpAttempts, PasswordOtpIssuanceService otpIssuance,
            PasswordChangeTransactionService passwordChanges,
            @Value("${app.support.recipient-email}") String supportRecipient,
            @Value("${app.support.from-email}") String supportFrom) {
        this.students = students; this.authorization = authorization;
        this.passwordEncoder = passwordEncoder; this.mailSenders = mailSenders;
        this.otpAttempts = otpAttempts; this.otpIssuance = otpIssuance; this.passwordChanges = passwordChanges;
        this.supportRecipient = supportRecipient; this.supportFrom = supportFrom;
    }

    @Transactional(readOnly = true)
    public List<DirectoryStudentResponse> directory(Authentication authentication, String search) {
        authorization.student(authentication);
        String normalized = search == null || search.isBlank() ? null : search.trim();
        return students.findStudentDirectory(normalized).stream()
                .map(s -> new DirectoryStudentResponse(s.getStudentId(), s.getFullName(), s.getEnrollmentNumber(),
                        s.getAcademicProgram(), s.getDepartmentName(),
                        s.getProfilePhotoKey() == null ? null
                                : "/student/directory/" + s.getStudentId() + "/profile-photo")).toList();
    }

    @Transactional
    public void submitComplaint(Authentication authentication, ComplaintRequest request) {
        Student student = authorization.student(authentication);
        if (request == null) throw bad("Complaint details are required");
        String subject = required(request.subject(), "Subject");
        String message = required(request.message(), "Message");
        if (subject.length() > 200) throw bad("Subject cannot exceed 200 characters");
        if (message.length() > 10000) throw bad("Message cannot exceed 10000 characters");
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null) throw unavailable("Support email is not configured");
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(supportRecipient);
        mail.setFrom(supportFrom);
        mail.setSubject("CampusHire complaint: " + subject);
        mail.setText("Student: " + student.getUser().getFullName() + "\n"
                + "University email: " + student.getUser().getUniversityEmail() + "\n"
                + "Enrollment number: " + student.getEnrollmentNumber() + "\n\n" + message);
        try { sender.send(mail); }
        catch (RuntimeException exception) { throw unavailable("The complaint could not be delivered. Please try again later"); }
    }

    @Transactional
    public void requestOtp(Authentication authentication) {
        Student student = authorization.student(authentication);
        Instant now = Instant.now();
        String otp = String.format("%06d", random.nextInt(1_000_000));
        PasswordChangeOtp challenge = otpIssuance.issue(student.getUser().getId(), passwordEncoder.encode(otp), now)
                .orElseThrow(() -> bad("Please wait before requesting another OTP"));
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null) throw unavailable("Password email service is not configured");
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(student.getUser().getUniversityEmail());
        mail.setFrom(supportFrom);
        mail.setSubject("CampusHire password change OTP");
        mail.setText("Your CampusHire password-change OTP is " + otp + ". It expires in 5 minutes.");
        try { sender.send(mail); }
        catch (RuntimeException exception) { challenge.markUsed(); throw unavailable("The OTP could not be delivered. Please try again later"); }
    }

    @Transactional
    public void verifyOtp(Authentication authentication, OtpRequest request) {
        Student student = authorization.student(authentication);
        if (request == null || request.otp() == null || !request.otp().matches("\\d{6}")) throw bad("Enter the 6-digit OTP");
        PasswordOtpAttemptService.Result result = otpAttempts.verify(student.getUser().getId(), request.otp());
        switch (result) {
            case VERIFIED -> { return; }
            case INVALID -> throw bad("The OTP is invalid");
            case LOCKED -> throw bad("Too many OTP attempts. Request a new OTP");
            case EXPIRED -> throw bad("The OTP has expired");
            case ALREADY_VERIFIED -> throw bad("This OTP has already been verified");
            default -> throw bad("Request a password-change OTP first");
        }
    }

    @Transactional
    public void changePassword(Authentication authentication, ChangePasswordRequest request) {
        Student student = authorization.student(authentication);
        if (request == null) throw bad("Password details are required");
        if (request.newPassword() == null || request.newPassword().length() < 8) throw bad("Password must be at least 8 characters");
        if (!request.newPassword().equals(request.confirmPassword())) throw bad("Passwords do not match");
        PasswordChangeTransactionService.Result result = passwordChanges.change(student.getUser().getId(),
                passwordEncoder.encode(request.newPassword()));
        switch (result) {
            case CHANGED -> { return; }
            case EXPIRED -> throw bad("The OTP has expired");
            case NOT_VERIFIED -> throw bad("Verify the OTP before changing your password");
            default -> throw bad("Request a password-change OTP first");
        }
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) throw bad(field + " is required");
        return value.trim();
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException unavailable(String message) { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message); }
}
