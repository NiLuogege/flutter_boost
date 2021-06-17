package com.idlefish.flutterboost;

/**
 * flutter boost 的配置
 */
public class FlutterBoostSetupOptions {
    private final String initialRoute; //初始路由 默认 /
    private final String dartEntrypoint; // dart 代码入口 默认为 main(main.dart)
    private final String[] shellArgs; // 在创建 FlutterEngine 时会用到 
    private final boolean shouldOverrideBackForegroundEvent; // 这是干啥的

    private FlutterBoostSetupOptions(Builder builder) {
        this.initialRoute = builder.initialRoute;
        this.dartEntrypoint = builder.dartEntrypoint;
        this.shellArgs = builder.shellArgs;
        this.shouldOverrideBackForegroundEvent = builder.shouldOverrideBackForegroundEvent;
    }

    public static FlutterBoostSetupOptions createDefault() {
      return new Builder().build();
    }

    public String initialRoute() {
        return initialRoute;
    }

    public String dartEntrypoint() {
        return dartEntrypoint;
    }

    public String[] shellArgs() {
        return shellArgs;
    }

    public boolean shouldOverrideBackForegroundEvent() {
        return shouldOverrideBackForegroundEvent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        if (shellArgs == null || shellArgs.length == 0) {
            sb.append(']');
        } else {
            for (int i = 0; ; i++) {
                sb.append(String.valueOf(shellArgs[i]));
                if (i == shellArgs.length - 1) {
                    sb.append(']');
                    break;
                }
                sb.append(", ");
            }
        }
        return "initialRoute:" + this.initialRoute +
                ", dartEntrypoint:" + this.dartEntrypoint +
                ", shouldOverrideBackForegroundEvent:" + this.shouldOverrideBackForegroundEvent +
                ", shellArgs:" + sb.toString();
    }

    public static class Builder {
        private String initialRoute = "/";
        private String dartEntrypoint = "main";
        private boolean shouldOverrideBackForegroundEvent = false;
        private String[] shellArgs;

        public Builder() {
        }

        public Builder initialRoute(String initialRoute){
            this.initialRoute = initialRoute;
            return this;
        }

        public Builder dartEntrypoint(String dartEntrypoint){
            this.dartEntrypoint = dartEntrypoint;
            return this;
        }

        public Builder shellArgs(String[] shellArgs){
            this.shellArgs = shellArgs;
            return this;
        }

        // Determines whether to override back/foreground event.
        public Builder shouldOverrideBackForegroundEvent(boolean override) {
            shouldOverrideBackForegroundEvent = override;
            return this;
        }

        public FlutterBoostSetupOptions build() {
            FlutterBoostSetupOptions options = new FlutterBoostSetupOptions(this);
            return options;
        }
    }
}