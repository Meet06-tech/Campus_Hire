class PlacementCompany {
  const PlacementCompany({
    required this.id,
    required this.name,
    this.description,
    this.website,
    this.hrContactName,
    this.hrContactEmail,
    this.hrContactPhone,
  });

  final int id;
  final String name;
  final String? description;
  final String? website;
  final String? hrContactName;
  final String? hrContactEmail;
  final String? hrContactPhone;

  factory PlacementCompany.fromJson(Map<String, dynamic> json) {
    return PlacementCompany(
      id: (json['id'] as num).toInt(),
      name: (json['name'] ?? '').toString(),
      description: json['description']?.toString(),
      website: json['website']?.toString(),
      hrContactName: json['hrContactName']?.toString(),
      hrContactEmail: json['hrContactEmail']?.toString(),
      hrContactPhone: json['hrContactPhone']?.toString(),
    );
  }
}

class PlacementDrive {
  const PlacementDrive({
    required this.id,
    required this.company,
    required this.title,
    required this.status,
    this.departmentId,
    this.jobDescription,
    this.jobType,
    this.location,
    this.packageSalary,
    this.applicationDeadline,
    this.minimumCgpa,
    this.graduationYear,
    this.maximumActiveBacklogs,
    this.eligibleProgramCodes = const [],
  });

  final int id;
  final PlacementCompany company;
  final int? departmentId;
  final String title;
  final String? jobDescription;
  final String? jobType;
  final String? location;
  final String? packageSalary;
  final DateTime? applicationDeadline;
  final String status;
  final double? minimumCgpa;
  final int? graduationYear;
  final int? maximumActiveBacklogs;
  final List<String> eligibleProgramCodes;

  factory PlacementDrive.fromJson(Map<String, dynamic> json) {
    final rawCompany = json['company'];
    final rawPrograms = json['eligibleProgramCodes'];
    return PlacementDrive(
      id: (json['id'] as num).toInt(),
      company: PlacementCompany.fromJson(
        Map<String, dynamic>.from(rawCompany as Map),
      ),
      departmentId: (json['departmentId'] as num?)?.toInt(),
      title: (json['title'] ?? '').toString(),
      jobDescription: json['jobDescription']?.toString(),
      jobType: json['jobType']?.toString(),
      location: json['location']?.toString(),
      packageSalary: json['packageSalary']?.toString(),
      applicationDeadline: _parseDate(json['applicationDeadline']),
      status: (json['status'] ?? '').toString(),
      minimumCgpa: (json['minimumCgpa'] as num?)?.toDouble(),
      graduationYear: (json['graduationYear'] as num?)?.toInt(),
      maximumActiveBacklogs: (json['maximumActiveBacklogs'] as num?)?.toInt(),
      eligibleProgramCodes: rawPrograms is List
          ? rawPrograms.map((value) => value.toString()).toList(growable: false)
          : const [],
    );
  }
}

class PlacementApplication {
  const PlacementApplication({
    required this.id,
    required this.driveId,
    required this.studentId,
    required this.appliedAt,
    required this.status,
  });

  final int id;
  final int driveId;
  final int studentId;
  final DateTime appliedAt;
  final String status;

  factory PlacementApplication.fromJson(Map<String, dynamic> json) {
    return PlacementApplication(
      id: (json['id'] as num).toInt(),
      driveId: (json['driveId'] as num).toInt(),
      studentId: (json['studentId'] as num).toInt(),
      appliedAt: DateTime.parse((json['appliedAt'] ?? '').toString()),
      status: (json['status'] ?? '').toString(),
    );
  }
}

DateTime? _parseDate(Object? value) {
  if (value == null) return null;
  return DateTime.tryParse(value.toString());
}
