package com.placementmanagement.api.auth.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.placementmanagement.api.auth.service.ProfilePhotoService;
import com.placementmanagement.api.auth.service.ProfilePhotoService.PhotoFile;

@RestController
@RequestMapping("/api/me/profile-photo")
public class  ProfilePhotoController {
    private final ProfilePhotoService profilePhotoService;

    public ProfilePhotoController(ProfilePhotoService profilePhotoService) {
        this.profilePhotoService = profilePhotoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestPart("file") MultipartFile file, Authentication authentication) {
        return profilePhotoService.upload(authentication, file);
    }

    @GetMapping(produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> read(Authentication authentication) {
        PhotoFile photo = profilePhotoService.current(authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .body(photo.content());
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(Authentication authentication) {
        profilePhotoService.remove(authentication);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
