import 'package:logger/logger.dart' as Log;

// ignore: avoid_classes_with_only_static_members
class Logger {
  static var logger = Log.Logger(printer: Log.PrettyPrinter());

  static void log(String msg) {
    logger.e('$msg');
  }

  static void error(String msg) {
    // print('FlutterBoost#$msg');
    logger.e('$msg');
  }
}
