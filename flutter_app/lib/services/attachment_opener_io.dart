import 'package:open_filex/open_filex.dart';

Future<bool> openAttachment(String path) async =>
    (await OpenFilex.open(path)).type == ResultType.done;
