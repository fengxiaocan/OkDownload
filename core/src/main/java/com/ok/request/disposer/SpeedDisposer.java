package com.ok.request.disposer;

import com.ok.request.base.DownloadExecutor;
import com.ok.request.dispatch.OnDownloadDispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.listener.OnSpeedListener;

import java.util.concurrent.atomic.AtomicLong;

public final class SpeedDisposer {
    private final boolean ignoredSpeed;
    private final long updateSpeedTimes;
    private final AtomicLong lastTime = new AtomicLong();
    private final OnSpeedListener onSpeedListener;
    private final Schedulers schedulers;

    public SpeedDisposer(boolean ignoredSpeed, long updateSpeedTimes, OnDownloadDispatcher dispatcher) {
        this.ignoredSpeed = ignoredSpeed;
        this.updateSpeedTimes = updateSpeedTimes;
        this.onSpeedListener = dispatcher.speedListener();
        this.schedulers = dispatcher.schedulers();
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

    public void onSpeed(final DownloadExecutor executor, final int speed) {
        if (onSpeedListener != null) {
            final int gap = (int) gap();

            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        onSpeedListener.onSpeed(executor, speed, gap);
                    }
                });
            } else {
                onSpeedListener.onSpeed(executor, speed, gap);
            }
        }
        lastTime.set(System.currentTimeMillis());
    }
}
