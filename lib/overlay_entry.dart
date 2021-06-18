import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_boost/logger.dart';

import 'boost_container.dart';

//保存了全局的 OverlayState
final GlobalKey<OverlayState> overlayKey = GlobalKey<OverlayState>();
// OverlayEntry(所有页面) 的栈？
List<_ContainerOverlayEntry> _lastEntries = <_ContainerOverlayEntry>[];

///The Entry refresh mode,which indicates different situation
enum BoostSpecificEntryRefreshMode {
  ///Just add an new entry
  add,

  ///remove a specific entry from entries list
  remove,

  ///move an existing entry to top
  moveToTop,
}

///Refresh an specific entry instead of all of entries to enhance the performace
///
///[container] : The container you want to operate, it is related with
///              internal [OverlayEntry]
///[mode] : The [BoostSpecificEntryRefreshMode] you want to choose
/// 刷新 BoostContainer 中的内容
void refreshSpecificOverlayEntries(
    BoostContainer container, BoostSpecificEntryRefreshMode mode) {
  //Get OverlayState from global key
  final overlayState = overlayKey.currentState;
  if (overlayState == null) {
    return;
  }

  final hasScheduledFrame = SchedulerBinding.instance.hasScheduledFrame;
  final framesEnabled = SchedulerBinding.instance.framesEnabled;

  //deal with different situation
  switch (mode) {
    case BoostSpecificEntryRefreshMode.add: // 添加
    //床架你一个 OverlayEntry
      final entry = _ContainerOverlayEntry(container);
      _lastEntries.add(entry);
      //添加一层 OverlayEntry
      overlayState.insert(entry);
      break;
    case BoostSpecificEntryRefreshMode.remove: // 移除
      if (_lastEntries.isNotEmpty) {
        //Find the entry matching the container
        final entryToRemove = _lastEntries.singleWhere((element) {
          return element.containerUniqueId == container.pageInfo.uniqueId;
        });

        //remove from the list and overlay
        _lastEntries.remove(entryToRemove);
        entryToRemove.remove();
      }
      break;
    case BoostSpecificEntryRefreshMode.moveToTop: // 移到顶部
      final existingEntry = _lastEntries.singleWhere((element) {
        return element.containerUniqueId == container.pageInfo.uniqueId;
      });
      //remove the entry from list and overlay
      //and insert it to list'top and overlay 's top
      _lastEntries.remove(existingEntry);
      _lastEntries.add(existingEntry);
      existingEntry.remove();
      overlayState.insert(existingEntry);
      break;
  }

  // https://github.com/alibaba/flutter_boost/issues/1056
  // Ensure this frame is refreshed after schedule frame,
  // otherwise the PageState.dispose may not be called
  if (hasScheduledFrame || !framesEnabled) {
    SchedulerBinding.instance.scheduleWarmUpFrame();
  }
}

///Refresh all of overlayEntries
void refreshAllOverlayEntries(List<BoostContainer> containers) {
  final overlayState = overlayKey.currentState;

  Logger.log('refreshAllOverlayEntries overlayState=$overlayState');
  Logger.log('refreshAllOverlayEntries _lastEntries=$_lastEntries');

  if (overlayState == null) {
    return;
  }

  if (_lastEntries != null && _lastEntries.isNotEmpty) {
    for (var entry in _lastEntries) {
      entry.remove();//移除所有  OverlayEntry
    }
  }

  _lastEntries = containers
      .map<_ContainerOverlayEntry>(
          (container) => _ContainerOverlayEntry(container))
      .toList(growable: true);

  final hasScheduledFrame = SchedulerBinding.instance.hasScheduledFrame;
  final framesEnabled = SchedulerBinding.instance.framesEnabled;

  Logger.log('refreshAllOverlayEntries new _lastEntries=$_lastEntries');

  //将所有 container 都封装为一个  _ContainerOverlayEntry（就是 OverlayEntry） 然后加到 overlayState
  overlayState.insertAll(_lastEntries);

  // 下面代码是为了 解决bug
  // https://github.com/alibaba/flutter_boost/issues/1056
  // Ensure this frame is refreshed after schedule frame，
  // otherwise the PageState.dispose may not be called
  if (hasScheduledFrame || !framesEnabled) {
    SchedulerBinding.instance.scheduleWarmUpFrame();
  }
}

//每个 _ContainerOverlayEntry 都包含一个 BoostContainerWidget
class _ContainerOverlayEntry extends OverlayEntry {
  _ContainerOverlayEntry(BoostContainer container)
      : containerUniqueId = container.pageInfo.uniqueId,
        super(
            builder: (ctx) => BoostContainerWidget(container: container),
            opaque: true,
            maintainState: true);

  ///This overlay's id,which is the same as the it's related container
  final String containerUniqueId;

  @override
  String toString() {
    return '_ContainerOverlayEntry: containerId:$containerUniqueId';
  }
}
