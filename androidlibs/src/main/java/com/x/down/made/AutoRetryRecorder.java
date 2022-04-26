package com.x.down.made;

import com.x.down.tool.XDownUtils;

public class AutoRetryRecorder {
    protected final boolean isUseAutoRetry;//是否使用出错自动重试
    protected final int autoRetryTimes;//自动重试次数
    protected final int autoRetryInterval;//自动重试间隔
    private volatile int retryCount = 0;//失败次数

    public AutoRetryRecorder(boolean isUseAutoRetry, int autoRetryTimes, int autoRetryInterval) {
        this.isUseAutoRetry = isUseAutoRetry;
        this.autoRetryTimes = autoRetryTimes;
        this.autoRetryInterval = autoRetryInterval;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public synchronized boolean isCanRetry() {
        if (isUseAutoRetry) {
            if (retryCount < autoRetryTimes) {
                retryCount++;
                return true;
            }
        }
        return false;
    }

    public void sleep() {
        if (autoRetryInterval > 1) {
            XDownUtils.sleep(autoRetryInterval);
        }
    }
}
