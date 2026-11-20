import 'dart:io';
import 'dart:typed_data';

import 'package:path_provider/path_provider.dart';

Future<String> saveAttachment(Uint8List bytes, String fileName) async {
  final directory = Directory(
    '${(await getApplicationDocumentsDirectory()).path}/CampusHire/Attachments',
  );
  await directory.create(recursive: true);
  final safeName = fileName.replaceAll(RegExp(r'[^A-Za-z0-9._ -]'), '_');
  final file = File('${directory.path}/$safeName');
  await file.writeAsBytes(bytes, flush: true);
  return file.path;
}
