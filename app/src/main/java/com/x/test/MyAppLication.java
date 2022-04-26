package com.x.test;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.x.down.XDownload;

public class MyAppLication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        XDownload.init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);

    }

}
