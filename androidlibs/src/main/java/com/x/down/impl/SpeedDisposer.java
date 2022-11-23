package com.x.down.impl;

import com.x.down.base.IDownloadRequest;

import java.util.concurrent.atomic.AtomicLong;

public final class SpeedDisposer {
    private final boolean ignoredSpeed;
    private final long updateSpeedTimes;
    private final DownloadListenerDisposer disposer;
    private final AtomicLong lastTime = new AtomicLong();

    public SpeedDisposer(
            boolean ignoredSpeed, long updateSpeedTimes, DownloadListenerDisposer listener) {
        this.ignoredSpeed = ignoredSpeed;
        this.updateSpeedTimes = updateSpeedTimes;
        this.disposer = listener;
    }

    public boolean isIgnoredSpeed() {
        return ignoredSpeed;
    }

    public boolean isCallSpeed() {
        if (ignoredSpeed || updateSpeedTimes <= 0) {
            return false;
        }
        return gap() >= updateSpeedTimes;
    }

    private long gap() {
        return System.currentTimeMillis() - lastTime.get();
    }

    public void onSpeed(IDownloadRequest request, final int speed) {
        disposer.onSpeed(request, speed, (int) gap());
        lastTime.set(System.currentTimeMillis());
    }
}
