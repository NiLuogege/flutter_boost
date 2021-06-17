package com.idlefish.flutterboost;

/**
 * 代理
 */
public interface FlutterBoostDelegate {
    void pushNativeRoute(FlutterBoostRouteOptions options);
    void pushFlutterRoute(FlutterBoostRouteOptions options);
}
