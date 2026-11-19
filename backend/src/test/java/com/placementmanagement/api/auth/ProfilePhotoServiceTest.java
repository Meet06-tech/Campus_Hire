package com.placementmanagement.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.service.CurrentUserService;
import com.placementmanagement.api.auth.service.ProfilePhotoService;
import com.placementmanagement.api.auth.service.ProfilePhotoStorageService;

class ProfilePhotoServiceTest {
    @Test
    void replacingOneStudentsPhotoLeavesOtherStudentsPhotoUnchanged() {
        UserAccount firstUser = mock(UserAccount.class);
        UserAccount secondUser = mock(UserAccount.class);
        Student firstStudent = mock(Student.class);
        Student secondStudent = mock(Student.class);
        AtomicReference<String> firstKey = new AtomicReference<>("first-old.jpg");
        when(firstUser.isEnabled()).thenReturn(true);
        when(firstUser.getRole()).thenReturn(UserRole.STUDENT);
        when(firstUser.getStudent()).thenReturn(firstStudent);
        when(secondUser.isEnabled()).thenReturn(true);
        when(secondUser.getRole()).thenReturn(UserRole.STUDENT);
        when(secondUser.getStudent()).thenReturn(secondStudent);
        when(firstStudent.getProfilePhotoKey()).thenAnswer(invocation -> firstKey.get());
        doAnswer(invocation -> {
            firstKey.set(invocation.getArgument(0));
            return null;
        }).when(firstStudent).updateProfilePhotoKey(anyString());
        when(secondStudent.getProfilePhotoKey()).thenReturn("second.jpg");

        UserAccountRepository users = mock(UserAccountRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(firstUser));
        when(users.findById(2L)).thenReturn(Optional.of(secondUser));
        StudentRepository students = mock(StudentRepository.class);
        ProfilePhotoStorageService storage = mock(ProfilePhotoStorageService.class);
        MultipartFile file = mock(MultipartFile.class);
        when(storage.store(file)).thenReturn("first-new.jpg");
        when(storage.read("first-new.jpg")).thenReturn(new byte[] {1});
        when(storage.read("second.jpg")).thenReturn(new byte[] {2});
        Authentication first = new UsernamePasswordAuthenticationToken("1", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        Authentication second = new UsernamePasswordAuthenticationToken("2", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        ProfilePhotoService service = new ProfilePhotoService(new CurrentUserService(users, students), storage, students);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.upload(first, file);
            assertArrayEquals(new byte[] {1}, service.current(first).content());
            assertArrayEquals(new byte[] {2}, service.current(second).content());
            assertEquals("second.jpg", secondStudent.getProfilePhotoKey());
            verify(students).save(firstStudent);
            verify(students, never()).save(secondStudent);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void staffCannotUseStudentPhotoOperations() {
        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(true);
        when(user.getRole()).thenReturn(UserRole.DTPO);

        UserAccountRepository repository = mock(UserAccountRepository.class);
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        CurrentUserService currentUser = new CurrentUserService(repository, mock(StudentRepository.class));
        ProfilePhotoService service = new ProfilePhotoService(currentUser,
                mock(ProfilePhotoStorageService.class), mock(StudentRepository.class));
        Authentication authentication = new UsernamePasswordAuthenticationToken("7", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_DTPO")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.current(authentication));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void directoryPhotoUsesTheRequestedDirectoryStudentAndRequiresStudentViewer() {
        UserAccount viewer = mock(UserAccount.class);
        when(viewer.isEnabled()).thenReturn(true);
        when(viewer.getRole()).thenReturn(UserRole.STUDENT);
        when(viewer.getStudent()).thenReturn(mock(Student.class));
        UserAccountRepository users = mock(UserAccountRepository.class);
        when(users.findById(7L)).thenReturn(Optional.of(viewer));

        Student directoryStudent = mock(Student.class);
        when(directoryStudent.getProfilePhotoKey()).thenReturn("directory-photo.jpg");
        StudentRepository students = mock(StudentRepository.class);
        when(students.findById(22L)).thenReturn(Optional.of(directoryStudent));
        ProfilePhotoStorageService storage = mock(ProfilePhotoStorageService.class);
        when(storage.read("directory-photo.jpg")).thenReturn(new byte[] {1, 2, 3});
        when(storage.contentType("directory-photo.jpg")).thenReturn("image/jpeg");

        ProfilePhotoService service = new ProfilePhotoService(new CurrentUserService(users, students), storage, students);
        Authentication authentication = new UsernamePasswordAuthenticationToken("7", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        ProfilePhotoService.PhotoFile photo = service.directoryPhoto(authentication, 22L);

        assertArrayEquals(new byte[] {1, 2, 3}, photo.content());
        verify(students).findById(22L);
    }

    @Test
    void sharedLegacyPhotoIsNotDeletedWhenAnotherStudentStillReferencesIt() {
        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(true);
        when(user.getRole()).thenReturn(UserRole.STUDENT);
        Student student = mock(Student.class);
        when(student.getUser()).thenReturn(user);
        when(student.getId()).thenReturn(7L);
        when(student.getProfilePhotoKey()).thenReturn("shared.jpg");

        UserAccountRepository users = mock(UserAccountRepository.class);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(user.getStudent()).thenReturn(student);
        StudentRepository students = mock(StudentRepository.class);
        when(students.existsByProfilePhotoKeyAndIdNot("shared.jpg", 7L)).thenReturn(true);
        ProfilePhotoStorageService storage = mock(ProfilePhotoStorageService.class);
        ProfilePhotoService service = new ProfilePhotoService(new CurrentUserService(users, students), storage, students);
        Authentication authentication = new UsernamePasswordAuthenticationToken("7", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.remove(authentication);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(storage, never()).deleteQuietly("shared.jpg");
    }

}
