package com.placementmanagement.api.student.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.placementmanagement.api.student.dto.StudentFeatureDtos.ChangePasswordRequest;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.ComplaintRequest;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.DirectoryStudentResponse;
import com.placementmanagement.api.student.dto.StudentFeatureDtos.OtpRequest;
import com.placementmanagement.api.student.service.StudentFeatureService;

@RestController
@RequestMapping("/api/student")
public class StudentFeatureController {
    private final StudentFeatureService service;
    public StudentFeatureController(StudentFeatureService service) { this.service = service; }

    @GetMapping("/directory")
    public List<DirectoryStudentResponse> directory(@RequestParam(required = false) String search, Authentication authentication) {
        return service.directory(authentication, search);
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
