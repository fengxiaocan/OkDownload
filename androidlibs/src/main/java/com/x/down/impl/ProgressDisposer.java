package com.x.down.impl;

import com.x.down.base.IDownloadRequest;

public final class ProgressDisposer {
    private final boolean ignoredProgress;
    private final long updateProgressTimes;
    private final DownloadListenerDisposer disposer;
    private volatile long lastTime;

    public ProgressDisposer(
            boolean ignoredProgress, long updateProgressTimes, DownloadListenerDisposer listener) {
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
        synchronized (Object.class) {
            final long l = System.currentTimeMillis() - lastTime;
            return l >= updateProgressTimes;
        }
    }

    public void onProgress(IDownloadRequest request, final long totalLength, final long sofarLength) {
        if (totalLength > 0) {
            lastTime = System.currentTimeMillis();
            disposer.onProgress(request, sofarLength * 1F / totalLength);
        }
    }
}
