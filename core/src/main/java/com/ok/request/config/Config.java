package com.ok.request.config;

public class Config {
    public static boolean ANDROID_SDK_VER29 = false;

    private static XConfig setting;

    public static void initVersion(boolean isSDK_VER29){
        Config.ANDROID_SDK_VER29 = isSDK_VER29;
    }

    public static synchronized XConfig config() {
        if (setting == null) {
            throw new NullPointerException("请先使用 CoreDownload.init() 初始化");
        }
        return setting;
    }

    public static void initSetting(XConfig config) {
        setting = config;
    }
}
