class RegistrationVerification {
  const RegistrationVerification({
    required this.registrationId,
    required this.maskedEmail,
    required this.expiresInSeconds,
  });

  final int registrationId;
  final String maskedEmail;
  final int expiresInSeconds;

  factory RegistrationVerification.fromJson(Map<String, dynamic> json) {
    return RegistrationVerification(
      registrationId: (json['registrationId'] as num).toInt(),
      maskedEmail: json['maskedEmail'] as String,
      expiresInSeconds: (json['expiresInSeconds'] as num?)?.toInt() ?? 300,
    );
  }
}
