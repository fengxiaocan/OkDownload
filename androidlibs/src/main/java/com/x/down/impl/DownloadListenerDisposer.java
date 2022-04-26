package com.x.down.impl;

import com.x.down.base.IDownloadRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnDownloadConnectListener;
import com.x.down.listener.OnDownloadListener;
import com.x.down.listener.OnProgressListener;
import com.x.down.listener.OnSpeedListener;
import com.x.down.tool.XDownUtils;

public final class DownloadListenerDisposer
        implements OnDownloadConnectListener, OnDownloadListener, OnProgressListener, OnSpeedListener {
    private final Schedulers schedulers;
    private final OnDownloadListener onDownloadListener;
    private final OnProgressListener onProgressListener;
    private final OnSpeedListener onSpeedListener;
    private final OnDownloadConnectListener onConnectListener;

    public DownloadListenerDisposer(XDownloadRequest request) {
        this.schedulers = request.getSchedulers();
        this.onConnectListener = request.getOnDownloadConnectListener();
        this.onDownloadListener = request.getOnDownloadListener();
        this.onProgressListener = request.getOnProgressListener();
        this.onSpeedListener = request.getOnSpeedListener();
    }

    @Override
    public void onPending(final IDownloadRequest request) {
        if (onConnectListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onConnectListener.onPending(request);
                }
            });
        } else {
            onConnectListener.onPending(request);
        }
    }

    @Override
    public void onStart(final IDownloadRequest request) {
        if (onConnectListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onConnectListener.onStart(request);
                }
            });
        } else {
            onConnectListener.onStart(request);
        }
    }

    @Override
    public void onConnecting(final IDownloadRequest request) {
        if (onConnectListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onConnectListener.onConnecting(request);
                }
            });
        } else {
            onConnectListener.onConnecting(request);
        }
    }

    @Override
    public void onRequestError(final IDownloadRequest request, final int code, final String error) {
        if (onConnectListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onConnectListener.onRequestError(request, code, error);
                }
            });
        } else {
            onConnectListener.onRequestError(request, code, error);
        }
    }

    @Override
    public void onCancel(final IDownloadRequest request) {
        if (onConnectListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onConnectListener.onCancel(request);
                }
            });
        } else {
            onConnectListener.onCancel(request);
        }
    }

    @Override
    public void onRetry(final IDownloadRequest request) {
        if (onConnectListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onConnectListener.onRetry(request);
                }
            });
        } else {
            onConnectListener.onRetry(request);
        }
    }

    @Override
    public void onComplete(final IDownloadRequest request) {
        XDownUtils.deleteDir(XDownUtils.getTempCacheDir(request.request()));
        if (onDownloadListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onDownloadListener.onComplete(request);
                }
            });
        } else {
            onDownloadListener.onComplete(request);
        }
    }

    @Override
    public void onFailure(final IDownloadRequest request) {
        if (onDownloadListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onDownloadListener.onFailure(request);
                }
            });
        } else {
            onDownloadListener.onFailure(request);
        }
    }

    @Override
    public void onProgress(final IDownloadRequest request, final float progress) {
        if (onProgressListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onProgressListener.onProgress(request, progress);
                }
            });
        } else {
            onProgressListener.onProgress(request, progress);
        }
    }

    @Override
    public void onSpeed(final IDownloadRequest request, final int speed, final int time) {
        if (onSpeedListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onSpeedListener.onSpeed(request, speed, time);
                }
            });
        } else {
            onSpeedListener.onSpeed(request, speed, time);
        }
    }
}
