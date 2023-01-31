package com.ok.request.config;

import java.io.File;

public class Config {
    private static XConfig setting;

    public static synchronized XConfig config() {
        if (setting == null) {
            String cachePath = new File(System.getProperty("user.dir"), "xDownload").getAbsolutePath();
            XConfig setting = new XConfig(cachePath);
            File recordDir = new File(System.getProperty("user.dir"), ".RECORD_TEMP");
            setting.recordDir(recordDir.getAbsolutePath());
            Config.setting = setting;
        }
        return setting;
    }

    public static void initSetting(XConfig config) {
        setting = config;
    }
}
