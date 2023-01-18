package com.ok.test;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.ok.request.OkDownload;

public class MyAppLication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        OkDownload.init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);

    }

}
