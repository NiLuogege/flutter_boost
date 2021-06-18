import 'package:flutter/widgets.dart';
import 'package:flutter/scheduler.dart';
import 'logger.dart';

/// 这里 处理了 flutter app 生命周期相关
mixin BoostFlutterBinding on WidgetsFlutterBinding {
  bool _appLifecycleStateLocked = true;

  @override
  void initInstances() {
    super.initInstances();
    _instance = this;
    //告诉flutter 体统 APP resumed
    changeAppLifecycleState(AppLifecycleState.resumed);
  }

  static BoostFlutterBinding get instance => _instance;
  static BoostFlutterBinding _instance;

  @override
  void handleAppLifecycleStateChanged(AppLifecycleState state) {
    if (_appLifecycleStateLocked) {
      return;
    }
    Logger.log('boost_flutter_binding: '
        'handleAppLifecycleStateChanged ${state.toString()}');
    super.handleAppLifecycleStateChanged(state);
  }

  void changeAppLifecycleState(AppLifecycleState state) {
    if (SchedulerBinding.instance.lifecycleState == state) {
      return;
    }
    _appLifecycleStateLocked = false;
    handleAppLifecycleStateChanged(state);
    _appLifecycleStateLocked = true;
  }
}
