class Announcement {
  const Announcement({
    required this.id,
    required this.subject,
    required this.title,
    required this.message,
    required this.publisherName,
    required this.publisherRole,
    required this.publishedAt,
    this.departmentName,
  });

  final int id;
  final String subject;
  final String title;
  final String message;
  final String publisherName;
  final String publisherRole;
  final DateTime publishedAt;
  final String? departmentName;

  factory Announcement.fromJson(Map<String, dynamic> json) {
    return Announcement(
      id: (json['id'] as num).toInt(),
      subject: (json['subject'] ?? '').toString(),
      title: (json['title'] ?? '').toString(),
      message: (json['message'] ?? '').toString(),
      publisherName: (json['publisherName'] ?? '').toString(),
      publisherRole: (json['publisherRole'] ?? '').toString(),
      publishedAt: DateTime.parse((json['publishedAt'] ?? '').toString()),
      departmentName: json['departmentName']?.toString(),
    );
  }
}
