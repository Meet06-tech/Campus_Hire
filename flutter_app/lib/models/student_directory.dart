class StudentDirectoryEntry {
  const StudentDirectoryEntry({
    required this.studentId,
    required this.fullName,
    required this.enrollmentNumber,
    this.academicProgram,
    required this.departmentName,
    this.profilePhotoUrl,
  });
  final int studentId;
  final String fullName;
  final String enrollmentNumber;
  final String? academicProgram;
  final String departmentName;
  final String? profilePhotoUrl;

  factory StudentDirectoryEntry.fromJson(Map<String, dynamic> json) =>
      StudentDirectoryEntry(
        studentId: (json['studentId'] as num).toInt(),
        fullName: (json['fullName'] ?? '').toString(),
        enrollmentNumber: (json['enrollmentNumber'] ?? '').toString(),
        academicProgram: json['academicProgram']?.toString(),
        departmentName: (json['departmentName'] ?? '').toString(),
        profilePhotoUrl: json['profilePhotoUrl']?.toString(),
      );
}
