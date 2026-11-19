package com.placementmanagement.api.auth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import com.placementmanagement.api.auth.controller.ProfilePhotoController;
import com.placementmanagement.api.auth.service.ProfilePhotoService;
import com.placementmanagement.api.auth.service.ProfilePhotoService.PhotoFile;
import com.placementmanagement.api.student.controller.StudentFeatureController;
import com.placementmanagement.api.student.service.StudentFeatureService;

class ProfilePhotoControllerTest {
    @Test
    void ownPhotoIsNeverCacheableAcrossAccounts() {
        ProfilePhotoService photos = mock(ProfilePhotoService.class);
        Authentication first = mock(Authentication.class);
        Authentication second = mock(Authentication.class);
        when(photos.current(first)).thenReturn(new PhotoFile(new byte[] {1}, "image/png"));
        when(photos.current(second)).thenReturn(new PhotoFile(new byte[] {2}, "image/png"));

        ProfilePhotoController controller = new ProfilePhotoController(photos);
        var firstResponse = controller.read(first);
        var secondResponse = controller.read(second);

        assertEquals("no-store", firstResponse.getHeaders().getCacheControl());
        assertEquals("no-store", secondResponse.getHeaders().getCacheControl());
        assertArrayEquals(new byte[] {1}, firstResponse.getBody());
        assertArrayEquals(new byte[] {2}, secondResponse.getBody());
    }

    @Test
    void directoryPhotoIsNotCacheable() {
        ProfilePhotoService photos = mock(ProfilePhotoService.class);
        Authentication viewer = mock(Authentication.class);
        when(photos.directoryPhoto(viewer, 17L))
                .thenReturn(new PhotoFile(new byte[] {3}, "image/jpeg"));

        var response = new StudentFeatureController(mock(StudentFeatureService.class), photos)
                .directoryPhoto(17L, viewer);

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertArrayEquals(new byte[] {3}, response.getBody());
    }
}
