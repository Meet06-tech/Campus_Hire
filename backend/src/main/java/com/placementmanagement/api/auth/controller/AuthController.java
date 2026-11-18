package com.placementmanagement.api.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import com.placementmanagement.api.auth.dto.AuthDtos.AuthResponse;
import com.placementmanagement.api.auth.dto.AuthDtos.LoginRequest;
import com.placementmanagement.api.auth.dto.AuthDtos.RegisterRequest;
import com.placementmanagement.api.auth.dto.ForgotPasswordDtos.ForgotPasswordRequest;
import com.placementmanagement.api.auth.dto.ForgotPasswordDtos.ForgotPasswordResponse;
import com.placementmanagement.api.auth.dto.ForgotPasswordDtos.ResetPasswordRequest;
import com.placementmanagement.api.auth.dto.ForgotPasswordDtos.VerifyForgotPasswordRequest;
import com.placementmanagement.api.auth.dto.ForgotPasswordDtos.VerifyForgotPasswordResponse;
import com.placementmanagement.api.auth.dto.RegistrationDtos.RegistrationCompleteResponse;
import com.placementmanagement.api.auth.dto.RegistrationDtos.RegistrationOtpRequest;
import com.placementmanagement.api.auth.dto.RegistrationDtos.RegistrationStartResponse;
import com.placementmanagement.api.auth.service.AuthService;
import com.placementmanagement.api.auth.service.RegistrationService;
import com.placementmanagement.api.auth.service.ForgotPasswordService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final RegistrationService registrationService;
    private final ForgotPasswordService forgotPasswordService;

    public AuthController(AuthService authService, RegistrationService registrationService,
            ForgotPasswordService forgotPasswordService) {
        this.authService = authService;
        this.registrationService = registrationService;
        this.forgotPasswordService = forgotPasswordService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationStartResponse register(@RequestBody RegisterRequest request) {
        return registrationService.begin(request);
    }

    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationCompleteResponse verifyRegistration(@RequestBody RegistrationOtpRequest request) {
        return registrationService.verify(request);
    }

    @PostMapping("/register/resend")
    public RegistrationStartResponse resendRegistration(@RequestBody RegistrationOtpRequest request) {
        return registrationService.resend(request == null ? null : request.registrationId());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleExpectedAuthError(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("message", exception.getReason() == null ? "Request could not be completed" : exception.getReason()));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return forgotPasswordService.request(request == null ? null : request.email());
    }

    @PostMapping("/forgot-password/verify-otp")
    public VerifyForgotPasswordResponse verifyForgotPassword(@RequestBody VerifyForgotPasswordRequest request) {
        return forgotPasswordService.verify(request == null ? null : request.email(), request == null ? null : request.otp());
    }

    @PostMapping("/forgot-password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetForgotPassword(@RequestBody ResetPasswordRequest request) {
        forgotPasswordService.reset(request);
    }
}

