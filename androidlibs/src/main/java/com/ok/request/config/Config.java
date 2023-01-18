package com.ok.request.config;

import android.os.Build;

public class Config {
    public static boolean ANDROID_SDK_VER29 = Build.VERSION.SDK_INT < 29;

    private static XConfig setting;

    public static synchronized XConfig config() {
        if (setting == null) {
            throw new NullPointerException("请先使用 XDownload.init(context) 初始化");
        }
        return setting;
    }

    public static void initSetting(XConfig config) {
        setting = config;
    }
}
