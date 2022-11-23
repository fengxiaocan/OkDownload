package com.x.down.made;

import com.x.down.tool.XDownUtils;

import java.util.concurrent.atomic.AtomicInteger;

public class AutoRetryRecorder {
    protected final boolean isUseAutoRetry;//是否使用出错自动重试
    protected final int autoRetryTimes;//自动重试次数
    protected final int autoRetryInterval;//自动重试间隔
    private final AtomicInteger retryCount = new AtomicInteger(0);//失败次数

    public AutoRetryRecorder(boolean isUseAutoRetry, int autoRetryTimes, int autoRetryInterval) {
        this.isUseAutoRetry = isUseAutoRetry;
        this.autoRetryTimes = autoRetryTimes;
        this.autoRetryInterval = autoRetryInterval;
    }

    public int getRetryCount() {
        return retryCount.get();
    }

    public synchronized boolean isCanRetry() {
        if (isUseAutoRetry) {
            return retryCount.getAndIncrement() < autoRetryTimes;
        }
        return false;
    }

    public void sleep() {
        if (autoRetryInterval > 1) {
            XDownUtils.sleep(autoRetryInterval);
        }
    }
}
