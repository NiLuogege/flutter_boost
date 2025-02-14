package com.idlefish.flutterboost;

import android.util.Log;
import android.util.SparseArray;

import com.idlefish.flutterboost.Messages.CommonParams;
import com.idlefish.flutterboost.Messages.FlutterRouterApi;
import com.idlefish.flutterboost.Messages.NativeRouterApi;
import com.idlefish.flutterboost.Messages.StackInfo;
import com.idlefish.flutterboost.containers.FlutterContainerManager;
import com.idlefish.flutterboost.containers.FlutterViewContainer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;

/**
 * flutter boost 插件
 * <p>
 * NativeRouterApi： flutter 调用 原生的 BasicMessageChannel
 * ActivityAware： flutter 框架中的类，可以使 flutterPlugin 感受到 activity 的生命周期
 */
public class FlutterBoostPlugin implements FlutterPlugin, NativeRouterApi, ActivityAware {
    private static final String TAG = FlutterBoostPlugin.class.getSimpleName();
    private FlutterEngine engine;
    private FlutterRouterApi channel;//原生调用 flutter的 channel
    private FlutterBoostDelegate delegate;
    private StackInfo dartStack;//flutter页面栈信息
    private SparseArray<String> pageNames;
    private int requestCode = 1000;

    private HashMap<String, LinkedList<EventListener>> listenersTable = new HashMap<>();

    public FlutterRouterApi getChannel() {
        return channel;
    }

    public void setDelegate(FlutterBoostDelegate delegate) {
        this.delegate = delegate;
    }

    public FlutterBoostDelegate getDelegate() {
        return delegate;
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        // 预制 binaryMessenger 来准备处理消息
        NativeRouterApi.setup(binding.getBinaryMessenger(), this);
        engine = binding.getFlutterEngine();
        //原生调用 flutter的 channel
        channel = new FlutterRouterApi(binding.getBinaryMessenger());
        pageNames = new SparseArray<String>();
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        engine = null;
        channel = null;
    }

    /**
     * flutter 打开原生页面
     * <p>
     * 将参数 封装为 FlutterBoostRouteOptions 并通过 FlutterBoostDelegate 回到给 APP 自己处理
     */
    @Override
    public void pushNativeRoute(CommonParams params) {
        if (delegate != null) {
            requestCode++;
            if (pageNames != null) {
                pageNames.put(requestCode, params.getPageName());
            }
            FlutterBoostRouteOptions options = new FlutterBoostRouteOptions.Builder()
                    .pageName(params.getPageName())
                    .arguments((Map<String, Object>) (Object) params.getArguments())
                    .requestCode(requestCode)
                    .build();
            delegate.pushNativeRoute(options);
        } else {
            throw new RuntimeException("FlutterBoostPlugin might *NOT* set delegate!");
        }
    }


    /**
     * 原生 打开flutter页面
     * <p>
     * 将参数 封装为 FlutterBoostRouteOptions 并通过 FlutterBoostDelegate 回到给 APP 自己处理
     */
    @Override
    public void pushFlutterRoute(CommonParams params) {
        if (delegate != null) {
            FlutterBoostRouteOptions options = new FlutterBoostRouteOptions.Builder()
                    .pageName(params.getPageName())
                    .uniqueId(params.getUniqueId())
                    .arguments((Map<String, Object>) (Object) params.getArguments())
                    .build();
            delegate.pushFlutterRoute(options);
        } else {
            throw new RuntimeException("FlutterBoostPlugin might *NOT* set delegate!");
        }
    }

    /**
     * 关闭一个页面，通过 uniqueId 找对对应 FlutterViewContainer 然后进行关闭
     */
    @Override
    public void popRoute(CommonParams params) {
        String uniqueId = params.getUniqueId();
        if (uniqueId != null) {
            FlutterViewContainer container = FlutterContainerManager.instance().findContainerById(uniqueId);
            if (container != null) {
                container.finishContainer((Map<String, Object>) (Object) params.getArguments());
            }
        } else {
            throw new RuntimeException("Oops!! The unique id is null!");
        }
    }

    /**
     * 获取 dartStack
     */
    @Override
    public StackInfo getStackFromHost() {
        if (dartStack == null) {
            return StackInfo.fromMap(new HashMap());
        }
        Log.v(TAG, "#getStackFromHost: " + dartStack);
        return dartStack;
    }

    /**
     * 报错 dartStack
     */
    @Override
    public void saveStackToHost(StackInfo arg) {
        dartStack = arg;
        Log.v(TAG, "#saveStackToHost: " + dartStack);
    }

    /**
     * 发送事件的原生
     */
    @Override
    public void sendEventToNative(CommonParams arg) {
        //deal with the event from flutter side
        String key = arg.getKey();
        Map<Object, Object> arguments = arg.getArguments();
        assert (key != null);

        if (arguments == null) {
            arguments = new HashMap<>();
        }

        List<EventListener> listeners = listenersTable.get(key);
        if (listeners == null) {
            return;
        }

        for (EventListener listener : listeners) {
            listener.onEvent(key, arguments);
        }
    }

    /**
     * 添加 EventListener 并返回 一个移除的 接口
     */
    ListenerRemover addEventListener(String key, EventListener listener) {
        assert (key != null && listener != null);

        LinkedList<EventListener> listeners = listenersTable.get(key);
        if (listeners == null) {
            listeners = new LinkedList<>();
            listenersTable.put(key, listeners);
        }
        listeners.add(listener);

        LinkedList<EventListener> finalListeners = listeners;
        return () -> finalListeners.remove(listener);
    }

    private void checkEngineState() {
        if (engine == null || !engine.getDartExecutor().isExecutingDart()) {
            throw new RuntimeException("The engine is not ready for use. " +
                    "The message may be drop silently by the engine. " +
                    "You should check 'DartExecutor.isExecutingDart()' first!");
        }
    }

    /**
     * 打开flutter 页面
     */
    public void pushRoute(String uniqueId, String pageName, Map<String, Object> arguments,
                          final FlutterRouterApi.Reply<Void> callback) {
        if (channel != null) {
            checkEngineState();
            CommonParams params = new CommonParams();
            params.setUniqueId(uniqueId);
            params.setPageName(pageName);
            params.setArguments((Map<Object, Object>) (Object) arguments);
            channel.pushRoute(params, reply -> {
                if (callback != null) {
                    callback.reply(null);
                }
            });
        } else {
            throw new RuntimeException("FlutterBoostPlugin might *NOT* have attached to engine yet!");
        }
    }

    public void popRoute(String uniqueId, final FlutterRouterApi.Reply<Void> callback) {
        if (channel != null) {
            checkEngineState();
            CommonParams params = new CommonParams();
            params.setUniqueId(uniqueId);
            channel.popRoute(params, reply -> {
                if (callback != null) {
                    callback.reply(null);
                }
            });
        } else {
            throw new RuntimeException("FlutterBoostPlugin might *NOT* have attached to engine yet!");
        }
    }

    public void removeRoute(String uniqueId, final FlutterRouterApi.Reply<Void> callback) {
        if (channel != null) {
            checkEngineState();
            CommonParams params = new CommonParams();
            params.setUniqueId(uniqueId);
            channel.removeRoute(params, reply -> {
                if (callback != null) {
                    callback.reply(null);
                }
            });
        } else {
            throw new RuntimeException("FlutterBoostPlugin might *NOT* have attached to engine yet!");
        }
    }

    //通知flutter侧 activity处于前台
    public void onForeground() {
        if (channel != null) {
            checkEngineState();
            CommonParams params = new CommonParams();
            channel.onForeground(params, reply -> {
            });
        } else {
            throw new RuntimeException("FlutterBoostPlugin might *NOT* have attached to engine yet!");
        }
        Log.v(TAG, "## onForeground: " + channel);
    }

    //通知flutter侧 activity处于后台
    public void onBackground() {
        if (channel != null) {
            checkEngineState();
            CommonParams params = new CommonParams();
            channel.onBackground(params, reply -> {
            });
        } else {
            throw new RuntimeException("FlutterBoostPlugin might *NOT* have attached to engine yet!");
        }
        Log.v(TAG, "## onBackground: " + channel);
    }

    //容器显示后的回掉
    public void onContainerShow(String uniqueId) {
        if (channel != null) {
            checkEngineState();
            CommonParams params = new CommonParams();
            params.setUniqueId(uniqueId);
            channel.onContainerShow(params, reply -> {
            });
        } else {
            throw new RuntimeException("FlutterBoostPlugin might *NOT* have attached to engine yet!");
        }
        Log.v(TAG, "## onContainerShow: " + channel);
    }

    public void onContainerHide(String uniqueId) {
        if (channel != null) {
            checkEngineState();
            CommonParams params = new CommonParams();
            params.setUniqueId(uniqueId);
            channel.onContainerHide(params, reply -> {
            });
        } else {
            throw new RuntimeException("FlutterBoostPlugin might *NOT* have attached to engine yet!");
        }
        Log.v(TAG, "## onContainerHide: " + channel);
    }

    /**
     * 有flutter 容器创建了
     */
    public void onContainerCreated(FlutterViewContainer container) {
        Log.v(TAG, "#onContainerCreated: " + container.getUniqueId());
        FlutterContainerManager.instance().addContainer(container.getUniqueId(), container);
        if (FlutterContainerManager.instance().getContainerSize() == 1) { //当有一个的时候 就改变flutter侧的 整体生命周期为 RESUMED
            FlutterBoost.instance().changeFlutterAppLifecycle(FlutterBoost.FLUTTER_APP_STATE_RESUMED);
        }
    }

    /**
     * 当 activity 或者 fragment resume 时会调用
     *
     * @param container
     */
    public void onContainerAppeared(FlutterViewContainer container) {
        String uniqueId = container.getUniqueId();

        Log.e(TAG, "onContainerAppeared 打开flutter 页面 uniqueId=" + uniqueId);

        //记录活跃的 FlutterViewContainer
        FlutterContainerManager.instance().activateContainer(uniqueId, container);
        //打开对应的flutter 页面
        pushRoute(uniqueId, container.getUrl(), container.getUrlParams(), reply -> {
        });
        onContainerShow(uniqueId);
    }

    /**
     * 当 activity 或者 fragment onPause时会调用
     *
     * @param container
     */
    public void onContainerDisappeared(FlutterViewContainer container) {
        String uniqueId = container.getUniqueId();
        onContainerHide(uniqueId);
    }


    /**
     * flutter 容器销毁了
     */
    public void onContainerDestroyed(FlutterViewContainer container) {
        String uniqueId = container.getUniqueId();
        removeRoute(uniqueId, reply -> {
        });
        FlutterContainerManager.instance().removeContainer(uniqueId);
        if (FlutterContainerManager.instance().getContainerSize() == 0) {//没有一个flutter 容器的时候 就是PAUSED 状态
            FlutterBoost.instance().changeFlutterAppLifecycle(FlutterBoost.FLUTTER_APP_STATE_PAUSED);
        }
    }

    /**
     * flutterPlugin 被添加到 Activity 时会被回调
     *
     * @param activityPluginBinding
     */
    @Override
    public void onAttachedToActivity(ActivityPluginBinding activityPluginBinding) {
        activityPluginBinding.addActivityResultListener((requestCode, resultCode, intent) -> {
            if (channel != null) {
                checkEngineState();
                CommonParams params = new CommonParams();
                String pageName = pageNames.get(requestCode);
                pageNames.remove(requestCode);
                if (null != pageName) {
                    params.setPageName(pageName);
                    if (intent != null) {
                        Map<Object, Object> result = FlutterBoostUtils.bundleToMap(intent.getExtras());
                        params.setArguments(result);
                    }
                    channel.onNativeResult(params, reply -> {
                    });
                }
            } else {
                throw new RuntimeException("FlutterBoostPlugin might *NOT* have attached to engine yet!");
            }
            return true;
        });
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding activityPluginBinding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }
}
