package com.ok.request.config;

import java.io.File;

public class Config {
    public static boolean ANDROID_SDK_VER29 = true;

    private static XConfig setting;

    public static synchronized XConfig config() {
        if (setting == null) {
            String cachePath = new File(System.getProperty("user.dir"), "XDownload").getAbsolutePath();
            XConfig setting = new XConfig(cachePath);
            File recordDir = new File(System.getProperty("user.dir"), ".RECORD_TEMP");
            setting.recordDir(recordDir.getAbsolutePath());
            com.ok.request.config.Config.setting = setting;
        }
        return setting;
    }

    public static void initSetting(XConfig config) {
        setting = config;
    }
}
