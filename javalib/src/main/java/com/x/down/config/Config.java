package com.x.down.config;

import java.io.File;

public class Config {
    private static XConfig setting;

    public static synchronized XConfig config() {
        if (setting == null) {
            String cachePath = new File(System.getProperty("user.dir"), "xDownload").getAbsolutePath();
            setting = new XConfig(cachePath);
        }
        return setting;
    }

    public static void initSetting(XConfig config) {
        setting = config;
    }
}
