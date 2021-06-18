import 'package:logger/logger.dart' as Log;

// ignore: avoid_classes_with_only_static_members
class Logger {
  static var logger = Log.Logger(printer: Log.PrettyPrinter(methodCount: 0));

  static void log(String msg) {
    assert(() {
      // print('FlutterBoost#$msg');
      logger.e('FlutterBoost#$msg');
      return true;
    }());
  }

  static void error(String msg) {
    // print('FlutterBoost#$msg');
    logger.e('FlutterBoost#$msg');
  }
}
