class StudentProfile {
  const StudentProfile({
    required this.userId,
    required this.fullName,
    required this.universityEmail,
    required this.enrollmentNumber,
    required this.role,
    required this.active,
    this.department,
    this.profile = const {},
    this.profilePhotoUrl,
  });

  final int userId;
  final String fullName;
  final String universityEmail;
  final String enrollmentNumber;
  final String role;
  final bool active;
  final Map<String, dynamic>? department;
  final Map<String, dynamic> profile;
  final String? profilePhotoUrl;

  factory StudentProfile.fromJson(Map<String, dynamic> json) {
    return StudentProfile(
      userId: (json['userId'] as num).toInt(),
      fullName: (json['fullName'] ?? '') as String,
      universityEmail: (json['universityEmail'] ?? '') as String,
      enrollmentNumber: (json['enrollmentNumber'] ?? '') as String,
      role: (json['role'] ?? '') as String,
      active: json['active'] == true,
      department: json['department'] == null
          ? null
          : Map<String, dynamic>.from(json['department'] as Map),
      profile: json['profile'] == null
          ? const {}
          : Map<String, dynamic>.from(json['profile'] as Map),
      profilePhotoUrl: json['profilePhotoUrl'] as String?,
    );
  }
}
