package com.x.down.task;

import com.x.down.made.AutoRetryRecorder;

abstract class BaseExecuteRequest implements Runnable {
    protected final AutoRetryRecorder autoRetryRecorder;
    protected volatile boolean isCancel = false;

    public BaseExecuteRequest(AutoRetryRecorder autoRetryRecorder) {
        this.autoRetryRecorder = autoRetryRecorder;
    }

    protected abstract void onExecute() throws Throwable;

    protected abstract void onRetry();

    protected abstract void onError(Throwable e);

    protected abstract void onCancel();

    protected final void runTask() {
        try {
            if (isCancel) {
                onCancel();
                return;
            }
            onExecute();
        } catch (Throwable e) {
            if (isCancel) {
                onCancel();
            } else {
                if (autoRetryRecorder.isCanRetry()) {
                    //回调重试
                    onRetry();
                    //决定是否延迟执行重试
                    autoRetryRecorder.sleep();
                    //自动重试下载
                    runTask();
                } else {
                    onError(e);
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            runTask();
        } catch (Exception e) {
            onError(e);
        }
    }
}
