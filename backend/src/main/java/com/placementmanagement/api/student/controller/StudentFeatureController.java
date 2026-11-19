package com.placementmanagement.api.student.controller;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.placementmanagement.api.student.dto.StudentFeatureDtos.ChangePasswordRequest;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.ComplaintRequest;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.DirectoryStudentResponse;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.OtpRequest;
import com.placementmanagement.api.student.service.StudentFeatureService;
import com.placementmanagement.api.auth.service.ProfilePhotoService;

@RestController
@RequestMapping("/api/student")
public class StudentFeatureController {
    private final StudentFeatureService service;
    private final ProfilePhotoService profilePhotos;
    public StudentFeatureController(StudentFeatureService service, ProfilePhotoService profilePhotos) {
        this.service = service;
        this.profilePhotos = profilePhotos;
    }

    @GetMapping("/directory")
    public List<DirectoryStudentResponse> directory(@RequestParam(required = false) String search, Authentication authentication) {
        return service.directory(authentication, search);
    }

    @GetMapping("/directory/{studentId}/profile-photo")
    public ResponseEntity<byte[]> directoryPhoto(@PathVariable Long studentId, Authentication authentication) {
        ProfilePhotoService.PhotoFile photo = profilePhotos.directoryPhoto(authentication, studentId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(photo.contentType())).body(photo.content());
    }

    @PostMapping("/support/complaints")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complaint(@RequestBody ComplaintRequest request, Authentication authentication) {
        service.submitComplaint(authentication, request);
    }

    @PostMapping("/password/otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestOtp(Authentication authentication) { service.requestOtp(authentication); }

    @PostMapping("/password/verify-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyOtp(@RequestBody OtpRequest request, Authentication authentication) { service.verifyOtp(authentication, request); }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) { service.changePassword(authentication, request); }
}
