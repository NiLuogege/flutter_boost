import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';

import 'boost_navigator.dart';
import 'flutter_boost_app.dart';
import 'logger.dart';

//flutter 侧的页面容器，页面内容就是 添加到这个里面的  ,也就是 一个  BoostPage 对应一个 BoostContainer
class BoostContainer {
  BoostContainer({this.key, this.pageInfo}) {
    //PageInfo 转换为 BoostPage 并添加到 pages 中
    pages.add(BoostPage.create(pageInfo));
  }

  static BoostContainer of(BuildContext context) {
    final state = context.findAncestorStateOfType<BoostContainerState>();
    return state.container;
  }

  final LocalKey key;

  final PageInfo pageInfo;

  final List<BoostPage<dynamic>> _pages = <BoostPage<dynamic>>[];

  List<BoostPage<dynamic>> get pages => _pages;

  BoostPage<dynamic> get topPage => pages.last;

  int get size => pages.length;

  NavigatorState get navigator => _navKey.currentState;
  final GlobalKey<NavigatorState> _navKey = GlobalKey<NavigatorState>();

  //执行刷新操作 其实就是调用了 BoostContainerState 的 setState 方法
  void refresh() {
    if (_refreshListener != null) {
      _refreshListener();
    }
  }

  VoidCallback _refreshListener; //就是个刷新方法

  @override
  String toString() => '${objectRuntimeType(this, 'BoostContainer')}(name:${pageInfo.pageName},'
      ' pages:$pages)';
}

/// BoostContainerWidget 整体是为了 接管flutter 系统的导航，
// ignore: public_member_api_docs
class BoostContainerWidget extends StatefulWidget {
  // ignore: public_member_api_docs
  BoostContainerWidget({LocalKey key, this.container}) : super(key: container.key);

  final BoostContainer container;

  @override
  State<StatefulWidget> createState() => BoostContainerState();

  @override
  // ignore: invalid_override_of_non_virtual_member
  bool operator ==(Object other) {
    if (other is BoostContainerWidget) {
      var otherWidget = other;
      return container.pageInfo.uniqueId == otherWidget.container.pageInfo.uniqueId;
    }
    return super == other;
  }

  @override
  // ignore: invalid_override_of_non_virtual_member
  int get hashCode => container.pageInfo.uniqueId.hashCode;
}

class BoostContainerState extends State<BoostContainerWidget> {
  BoostContainer get container => widget.container;

  void _updatePagesList() {
    container.pages.removeLast();
  }

  @override
  void initState() {
    super.initState();
    container._refreshListener = refreshContainer;
  }

  @override
  void didUpdateWidget(covariant BoostContainerWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget != widget) {
      oldWidget.container._refreshListener = null;
      container._refreshListener = refreshContainer;
    }
  }

  void refreshContainer() {
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    Logger.log("BoostContainerState pages=${widget.container.pages}");

    //HeroControllerScope 要求只能有一个 navigator（导航器）
    return HeroControllerScope(
        controller: HeroController(),
        child: NavigatorExt(
          key: widget.container._navKey,
          pages: List<Page<dynamic>>.of(widget.container.pages),
          onPopPage: (route, result) {
            Logger.log("onPopPage route$route result$result");
            if (route.didPop(result)) {
              _updatePagesList();
              return true;
            }
            return false;
          },
          observers: <NavigatorObserver>[
            BoostNavigatorObserver(),
          ],
        ));
  }

  @override
  void dispose() {
    container._refreshListener = null;
    super.dispose();
  }
}

class NavigatorExt extends Navigator {
  NavigatorExt({
    Key key,
    List<Page<dynamic>> pages,
    PopPageCallback onPopPage,
    List<NavigatorObserver> observers,
  }) : super(key: key, pages: pages, onPopPage: onPopPage, observers: observers);

  @override
  NavigatorState createState() => NavigatorExtState();
}

//当flutter 回退到flutter 的时候会走到这里，如果容器中只有一个flutter页面时 会直接关闭了
class NavigatorExtState extends NavigatorState {
  @override
  void pop<T extends Object>([T result]) {
    // Taking over container page
    Logger.log("NavigatorExtState canPop=${canPop()}");
    if (!canPop()) {
      BoostNavigator.instance.pop(result);
    } else {
      super.pop(result);
    }
  }
}
