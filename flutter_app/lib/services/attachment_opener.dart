import 'attachment_opener_stub.dart'
    if (dart.library.io) 'attachment_opener_io.dart'
    as platform;

Future<bool> openAttachment(String path) => platform.openAttachment(path);
