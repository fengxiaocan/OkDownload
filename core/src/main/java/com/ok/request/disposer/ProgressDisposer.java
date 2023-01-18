package com.ok.request.disposer;

import com.ok.request.base.DownloadExecutor;
import com.ok.request.dispatch.OnDownloadDispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.listener.OnProgressListener;

import java.util.concurrent.atomic.AtomicLong;

public final class ProgressDisposer {
    private final boolean ignoredProgress;
    private final long updateProgressTimes;
    private final OnProgressListener listener;
    private final Schedulers schedulers;
    private final AtomicLong lastTime = new AtomicLong(0);

    public ProgressDisposer(boolean ignoredProgress, long updateProgressTimes, OnDownloadDispatcher dispatcher) {
        this.ignoredProgress = ignoredProgress;
        this.updateProgressTimes = updateProgressTimes;
        this.listener = dispatcher.progressListener();
        this.schedulers = dispatcher.schedulers();
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

    public void onProgress(final DownloadExecutor executor, final long total, final long sofar) {
        onProgress(executor, sofar * 1F / total, total, sofar);
    }

    public void onProgress(final DownloadExecutor executor, final float progress, final long total, final long sofar) {
        if (total > 0) {
            lastTime.set(System.currentTimeMillis());
            if (listener == null) {
                return;
            }
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        listener.onProgress(executor, progress, total, sofar);
                    }
                });
            } else {
                listener.onProgress(executor, progress, total, sofar);
            }
        }
    }
}
