package com.ok.request.executor;

import com.ok.request.base.Executor;
import com.ok.request.dispatch.OnDispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.disposer.AutoRetryRecorder;
import com.ok.request.listener.OnExecuteListener;

public class BaseExecute {
    protected final Object tags;
    protected final Schedulers schedulers;
    private final AutoRetryRecorder autoRetryRecorder;
    private final OnExecuteListener onExecuteListener;

    public BaseExecute(OnDispatcher onDispatcher) {
        autoRetryRecorder = onDispatcher.recorder();
        schedulers = onDispatcher.schedulers();
        onExecuteListener = onDispatcher.executor();
        tags = onDispatcher.getTag();
    }

    public BaseExecute(AutoRetryRecorder recorder, OnDispatcher onDispatcher) {
        autoRetryRecorder = recorder;
        schedulers = onDispatcher.schedulers();
        onExecuteListener = onDispatcher.executor();
        tags = onDispatcher.getTag();
    }

    protected final AutoRetryRecorder recorder() {
        return autoRetryRecorder;
    }

    protected final void onStart(final Executor executor) {
        if (onExecuteListener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        onExecuteListener.onStart(executor);
                    }
                });
            } else {
                onExecuteListener.onStart(executor);
            }
        }
    }

    protected final void onCancel(final Executor executor) {
        if (onExecuteListener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        onExecuteListener.onCancel(executor);
                    }
                });
            } else {
                onExecuteListener.onCancel(executor);
            }
        }
    }

    protected final void onError(final Executor executor, final Throwable e) {
        if (onExecuteListener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        onExecuteListener.onError(executor, e);
                    }
                });
            } else {
                onExecuteListener.onError(executor, e);
            }
        }
    }

    /**
     * 请求正在重试
     *
     * @param executor
     * @param retryCoun
     */
    protected final void onRetry(final Executor executor, final int retryCoun) {
        if (onExecuteListener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        onExecuteListener.onRetry(executor, retryCoun);
                    }
                });
            } else {
                onExecuteListener.onRetry(executor, retryCoun);
            }
        }
    }

    protected final void onComplete(final Executor executor) {
        if (onExecuteListener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        onExecuteListener.onComplete(executor);
                    }
                });
            } else {
                onExecuteListener.onComplete(executor);
            }
        }
    }


}
