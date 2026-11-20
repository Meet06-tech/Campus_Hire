import 'dart:js_interop';
import 'dart:typed_data';

import 'package:web/web.dart' as web;

Future<String> saveAttachment(Uint8List bytes, String fileName) async {
  final blob = web.Blob([bytes.toJS].toJS);
  final url = web.URL.createObjectURL(blob);
  try {
    final anchor = web.HTMLAnchorElement();
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
  } finally {
    await Future<void>.delayed(const Duration(seconds: 1));
    web.URL.revokeObjectURL(url);
  }
  return 'your browser Downloads folder';
}
