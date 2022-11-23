package com.x.down.impl;

import com.x.down.base.IDownloadRequest;

import java.util.concurrent.atomic.AtomicLong;

public final class ProgressDisposer {
    private final boolean ignoredProgress;
    private final long updateProgressTimes;
    private final DownloadListenerDisposer disposer;
    private final AtomicLong lastTime = new AtomicLong(0);

    public ProgressDisposer(boolean ignoredProgress, long updateProgressTimes, DownloadListenerDisposer listener) {
        this.ignoredProgress = ignoredProgress;
        this.updateProgressTimes = updateProgressTimes;
        this.disposer = listener;
    }

    public boolean isIgnoredProgress() {
        return ignoredProgress;
    }

    public boolean isCallProgress() {
        if (ignoredProgress || updateProgressTimes <= 0) {
            return false;
        }
        final long l = System.currentTimeMillis() - lastTime.get();
        return l >= updateProgressTimes;
    }

    public void onProgress(IDownloadRequest request, final long total, final long sofar) {
        if (total > 0) {
            lastTime.set(System.currentTimeMillis());
            disposer.onProgress(request, sofar * 1F / total, total, sofar);
        }
    }

    public void onProgress(IDownloadRequest request, float progress, final long total, final long sofar) {
        if (total > 0) {
            lastTime.set(System.currentTimeMillis());
            disposer.onProgress(request, progress, total, sofar);
        }
    }
}
