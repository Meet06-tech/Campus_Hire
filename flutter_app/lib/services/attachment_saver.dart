import 'dart:typed_data';

import 'attachment_saver_stub.dart'
    if (dart.library.io) 'attachment_saver_io.dart'
    if (dart.library.html) 'attachment_saver_web.dart'
    as platform;

Future<String> saveAttachment(Uint8List bytes, String fileName) =>
    platform.saveAttachment(bytes, fileName);
