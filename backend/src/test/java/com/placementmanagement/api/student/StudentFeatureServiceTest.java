package com.placementmanagement.api.student;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.ChangePasswordRequest;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.ComplaintRequest;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.DirectoryStudentResponse;
import com.placementmanagement.api.student.service.PasswordChangeTransactionService;
import com.placementmanagement.api.student.service.PasswordOtpAttemptService;
import com.placementmanagement.api.student.service.PasswordOtpIssuanceService;
import com.placementmanagement.api.student.service.StudentFeatureService;

@ExtendWith(MockitoExtension.class)
class StudentFeatureServiceTest {
    @Mock StudentRepository students;
    @Mock PlacementAuthorizationService authorization;
    @Mock PasswordEncoder encoder;
    @Mock ObjectProvider<JavaMailSender> mailSenders;
    @Mock Authentication authentication;
    @Mock Student student;
    @Mock UserAccount user;
    @Mock Department department;
    @Mock PasswordOtpAttemptService otpAttempts;
    @Mock PasswordOtpIssuanceService otpIssuance;
    @Mock PasswordChangeTransactionService passwordChanges;

    private StudentFeatureService service() {
        return new StudentFeatureService(students, authorization, encoder, mailSenders,
                otpAttempts, otpIssuance, passwordChanges,
                "support@example.test", "campushire@example.test");
    }

    @Test
    void directoryRequiresStudentAndUsesSafeProjection() {
        when(authorization.student(authentication)).thenReturn(student);
        when(students.findStudentDirectory("mca")).thenReturn(List.of());
        service().directory(authentication, "mca");
        verify(students).findStudentDirectory("mca");
    }

    // ── Photo URL isolation tests ────────────────────────────────────────────

    /**
     * The photo URL in directory responses must NOT contain a leading /api segment.
     * Flutter's baseUrl already ends with /api, so the path must be relative to it
     * (e.g. /student/directory/{id}/profile-photo, not /api/student/…).
     * Without this invariant every student would hit a double-/api URL that returns
     * 404, and the photos would silently fail to load.
     */
    @Test
    void directoryPhotoUrlDoesNotContainLeadingApiSegment() {
        when(authorization.student(authentication)).thenReturn(student);

        StudentRepository.DirectoryProjection entry = mock(StudentRepository.DirectoryProjection.class);
        when(entry.getStudentId()).thenReturn(42L);
        when(entry.getFullName()).thenReturn("Alice");
        when(entry.getEnrollmentNumber()).thenReturn("20CE001");
        when(entry.getAcademicProgram()).thenReturn("CE");
        when(entry.getDepartmentName()).thenReturn("Computer Engineering");
        when(entry.getProfilePhotoKey()).thenReturn("some-photo.jpg");

        when(students.findStudentDirectory(null)).thenReturn(List.of(entry));

        List<DirectoryStudentResponse> results = service().directory(authentication, null);

        assertEquals(1, results.size());
        String url = results.get(0).profilePhotoUrl();
        assertNotNull(url, "profilePhotoUrl must not be null when a photo key exists");
        // The URL must NOT start with /api — Flutter's baseUrl already ends with /api
        assertNotEquals("/api/student/directory/42/profile-photo", url,
                "profilePhotoUrl must not contain a /api prefix (double-/api bug)");
        assertEquals("/student/directory/42/profile-photo", url,
                "profilePhotoUrl must be /student/directory/{id}/profile-photo");
    }

    /**
     * Each student in the directory must have their own unique photo URL that
     * encodes their own student ID, not a shared or constant value.
     */
    @Test
    void directoryPhotoUrlsAreIndependentPerStudent() {
        when(authorization.student(authentication)).thenReturn(student);

        StudentRepository.DirectoryProjection entryA = mock(StudentRepository.DirectoryProjection.class);
        when(entryA.getStudentId()).thenReturn(10L);
        when(entryA.getFullName()).thenReturn("Alice");
        when(entryA.getEnrollmentNumber()).thenReturn("20CE001");
        when(entryA.getAcademicProgram()).thenReturn("CE");
        when(entryA.getDepartmentName()).thenReturn("Computer Engineering");
        when(entryA.getProfilePhotoKey()).thenReturn("alice-photo.jpg");

        StudentRepository.DirectoryProjection entryB = mock(StudentRepository.DirectoryProjection.class);
        when(entryB.getStudentId()).thenReturn(20L);
        when(entryB.getFullName()).thenReturn("Bob");
        when(entryB.getEnrollmentNumber()).thenReturn("20CE002");
        when(entryB.getAcademicProgram()).thenReturn("CE");
        when(entryB.getDepartmentName()).thenReturn("Computer Engineering");
        when(entryB.getProfilePhotoKey()).thenReturn("bob-photo.jpg");

        when(students.findStudentDirectory(null)).thenReturn(List.of(entryA, entryB));

        List<DirectoryStudentResponse> results = service().directory(authentication, null);

        assertEquals(2, results.size());
        String urlA = results.get(0).profilePhotoUrl();
        String urlB = results.get(1).profilePhotoUrl();

        assertNotNull(urlA);
        assertNotNull(urlB);
        assertNotEquals(urlA, urlB, "Different students must have different photo URLs");
        assertEquals("/student/directory/10/profile-photo", urlA);
        assertEquals("/student/directory/20/profile-photo", urlB);
    }

    /**
     * A student without a profile photo key must receive null profilePhotoUrl,
     * so the Flutter client shows the default avatar instead of attempting a fetch.
     */
    @Test
    void directoryPhotoUrlIsNullWhenStudentHasNoPhoto() {
        when(authorization.student(authentication)).thenReturn(student);

        StudentRepository.DirectoryProjection entry = mock(StudentRepository.DirectoryProjection.class);
        when(entry.getStudentId()).thenReturn(99L);
        when(entry.getFullName()).thenReturn("Carol");
        when(entry.getEnrollmentNumber()).thenReturn("20CE003");
        when(entry.getAcademicProgram()).thenReturn("CE");
        when(entry.getDepartmentName()).thenReturn("Computer Engineering");
        when(entry.getProfilePhotoKey()).thenReturn(null);  // no photo

        when(students.findStudentDirectory(null)).thenReturn(List.of(entry));

        List<DirectoryStudentResponse> results = service().directory(authentication, null);

        assertEquals(1, results.size());
        assertNull(results.get(0).profilePhotoUrl(),
                "profilePhotoUrl must be null when the student has no profile photo");
    }

    @Test
    void blankComplaintIsRejectedBeforeEmailDelivery() {
        when(authorization.student(authentication)).thenReturn(student);
        assertThrows(ResponseStatusException.class, () -> service().submitComplaint(authentication,
                new ComplaintRequest(" ", "Message")));
        verifyNoInteractions(mailSenders);
    }

    @Test
    void passwordMismatchIsRejectedBeforeOtpLookup() {
        when(authorization.student(authentication)).thenReturn(student);
        assertThrows(ResponseStatusException.class, () -> service().changePassword(authentication,
                new ChangePasswordRequest("new-password", "different-password")));
        verifyNoInteractions(otpAttempts, passwordChanges);
    }

    @Test
    void recentOtpRequestIsRejectedBeforeEmailDelivery() {
        when(authorization.student(authentication)).thenReturn(student);
        when(student.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(7L);
        when(otpIssuance.issue(eq(7L), any(), any())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service().requestOtp(authentication));
        verifyNoInteractions(mailSenders);
    }
}
