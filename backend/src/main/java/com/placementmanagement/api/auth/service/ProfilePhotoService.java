package com.placementmanagement.api.auth.service;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.Student;

@Service
public class ProfilePhotoService {
    private final CurrentUserService currentUserService;
    private final ProfilePhotoStorageService storage;
    private final com.placementmanagement.api.auth.repository.StudentRepository students;

    public ProfilePhotoService(CurrentUserService currentUserService,
            ProfilePhotoStorageService storage,
            com.placementmanagement.api.auth.repository.StudentRepository students) {
        this.currentUserService = currentUserService;
        this.storage = storage;
        this.students = students;
    }

    @Transactional
    public Map<String, String> upload(org.springframework.security.core.Authentication authentication,
            MultipartFile file) {
        Student student = currentUserService.authenticatedStudent(authentication);
        String oldKey = student.getProfilePhotoKey();
        String newKey = storage.store(file);
        try {
            student.updateProfilePhotoKey(newKey);
            students.save(student);
            cleanupAfterCommit(newKey, oldKey);
            return Map.of("photoUrl", "/api/me/profile-photo");
        } catch (RuntimeException exception) {
            storage.deleteQuietly(newKey);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Profile photo could not be saved");
        }
    }

    @Transactional
    public void remove(org.springframework.security.core.Authentication authentication) {
        Student student = currentUserService.authenticatedStudent(authentication);
        String oldKey = student.getProfilePhotoKey();
        if (oldKey == null) return;
        try {
            student.updateProfilePhotoKey(null);
            students.save(student);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storage.deleteQuietly(oldKey);
                }
            });
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Profile photo could not be removed");
        }
    }

    private void cleanupAfterCommit(String newKey, String oldKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.deleteQuietly(oldKey);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) storage.deleteQuietly(newKey);
            }
        });
    }

    @Transactional(readOnly = true)
    public PhotoFile current(org.springframework.security.core.Authentication authentication) {
        Student student = currentUserService.authenticatedStudent(authentication);
        if (student.getProfilePhotoKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile photo not found");
        }
        String key = student.getProfilePhotoKey();
        return new PhotoFile(storage.read(key), storage.contentType(key));
    }

    public record PhotoFile(byte[] content, String contentType) {}
}
