package com.x.down.config;

public class Config {
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
