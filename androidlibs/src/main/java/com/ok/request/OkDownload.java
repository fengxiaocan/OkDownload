package com.ok.request;

import android.content.Context;
import android.os.Build;

import com.ok.request.config.Config;
import com.ok.request.config.IConfig;
import com.ok.request.config.UserAgent;
import com.ok.request.config.XConfig;

import java.io.File;

public final class OkDownload extends CoreDownload {
    private static Context context;

    private OkDownload() {
        throw new UnsupportedOperationException("XDownload no initialization is required!");
    }

    public static Context getContext() {
        return context;
    }

    public static IConfig init(Context context, String dirName) {
        Config.initVersion(Build.VERSION.SDK_INT < 29);

        File cache = new File(context.getExternalCacheDir(), dirName);
        File save = context.getExternalFilesDir(dirName);
        File recordDir = new File(context.getExternalCacheDir(), ".RECORD_TEMP");
        XConfig config = new XConfig(cache.getAbsolutePath());
        config.saveDir(save.getAbsolutePath());
        config.recordDir(recordDir.getAbsolutePath());
        config.userAgent(UserAgent.Android);
        OkDownload.config(config);
        return config;
    }

    public static IConfig init(Context context) {
        OkDownload.context = context;
        return init(context, "xDownload");
    }

    public static String getDefaultUserAgent() {
        //"Mozilla/5.0 (Linux; Android 10; MI 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.104 Mobile Safari/537.36";
        StringBuilder result = new StringBuilder("Mozilla/5.0 (Linux; Android ");
        result.append(Build.VERSION.RELEASE);
        result.append("; ");
        result.append(Build.MANUFACTURER);
        result.append(" ");
        if ("REL".equals(Build.VERSION.CODENAME)) {
            String model = Build.MODEL;
            if (model.length() > 0) {
                result.append(model);
            }
        }
        result.append(") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.104 Mobile Safari/537.36");
        return result.toString();
    }
}
