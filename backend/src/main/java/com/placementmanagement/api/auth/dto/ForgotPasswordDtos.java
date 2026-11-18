package com.placementmanagement.api.auth.dto;

public final class ForgotPasswordDtos {
    private ForgotPasswordDtos() {}

    public record ForgotPasswordRequest(String email) {}
    public record VerifyForgotPasswordRequest(String email, String otp) {}
    public record ResetPasswordRequest(String recoveryToken, String newPassword, String confirmPassword) {}
    public record ForgotPasswordResponse(String message) {}
    public record VerifyForgotPasswordResponse(String recoveryToken, long expiresInSeconds) {}
}
