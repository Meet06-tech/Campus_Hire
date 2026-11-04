class PlacementAttachment {
  const PlacementAttachment({required this.id, required this.fileName, required this.contentType, required this.sizeBytes});
  final int id;
  final String fileName;
  final String contentType;
  final int sizeBytes;
  factory PlacementAttachment.fromJson(Map<String, dynamic> json) => PlacementAttachment(
    id: (json['id'] as num).toInt(), fileName: (json['fileName'] ?? '').toString(),
    contentType: (json['contentType'] ?? 'application/octet-stream').toString(), sizeBytes: (json['sizeBytes'] as num?)?.toInt() ?? 0);
}
