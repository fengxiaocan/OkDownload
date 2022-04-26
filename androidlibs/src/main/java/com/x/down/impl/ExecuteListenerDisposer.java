package com.x.down.impl;

import com.x.down.base.IExecute;
import com.x.down.core.XExecuteRequest;
import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnExecuteListener;

public final class ExecuteListenerDisposer implements OnExecuteListener {
    protected final Schedulers schedulers;
    protected final OnExecuteListener onExecuteListener;

    public ExecuteListenerDisposer(XExecuteRequest request) {
        this.schedulers = request.getSchedulers();
        this.onExecuteListener = request.getExecuteListener();
    }

    @Override
    public void onPending(final IExecute request) {
        if (onExecuteListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onExecuteListener.onPending(request);
                }
            });
        } else {
            onExecuteListener.onPending(request);
        }
    }

    @Override
    public void onStart(final IExecute request) {
        if (onExecuteListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onExecuteListener.onStart(request);
                }
            });
        } else {
            onExecuteListener.onStart(request);
        }
    }

    @Override
    public void onCancel(final IExecute request) {
        if (onExecuteListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onExecuteListener.onCancel(request);
                }
            });
        } else {
            onExecuteListener.onCancel(request);
        }
    }

    @Override
    public void onRetry(final IExecute request) {
        if (onExecuteListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onExecuteListener.onRetry(request);
                }
            });
        } else {
            onExecuteListener.onRetry(request);
        }
    }

    @Override
    public void onError(final IExecute request, final Throwable exception) {
        if (onExecuteListener == null) {
            return;
        }
        if (schedulers != null) {
            schedulers.schedule(new Runnable() {
                @Override
                public void run() {
                    onExecuteListener.onError(request, exception);
                }
            });
        } else {
            onExecuteListener.onError(request, exception);
        }
    }
}
