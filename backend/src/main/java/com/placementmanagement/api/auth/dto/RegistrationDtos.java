package com.placementmanagement.api.auth.dto;

public final class RegistrationDtos {
    private RegistrationDtos() {}

    public record RegistrationStartResponse(Long registrationId, String maskedEmail, long expiresInSeconds) {}
    public record RegistrationOtpRequest(Long registrationId, String otp) {}
    public record RegistrationCompleteResponse(String message) {}
}
