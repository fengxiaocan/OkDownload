package com.x.down.task;


import com.x.down.made.AutoRetryRecorder;
import com.x.down.net.HttpErrorException;

import java.util.concurrent.atomic.AtomicBoolean;

abstract class BaseExecuteRequest implements Runnable {
    protected final AutoRetryRecorder autoRetryRecorder;
    private final AtomicBoolean cancelAtomic = new AtomicBoolean(false);

    public BaseExecuteRequest(AutoRetryRecorder autoRetryRecorder) {
        this.autoRetryRecorder = autoRetryRecorder;
    }

    protected abstract void onExecute() throws Throwable;

    protected abstract void onRetry();

    protected abstract void onError(Throwable e);

    protected abstract void onCancel();


    public final void cancelTask() {
        cancelAtomic.getAndSet(true);
    }

    public final void checkIsCancel() {
        if (cancelAtomic.get()) {
            throw new CancelTaskException();
        }
    }

    private void runTask() throws Throwable {
        try {
            onExecute();
        } catch (Throwable e) {
            if (e instanceof RetryTaskException) {
                retryTask();
                return;
            }
            if (e instanceof CancelTaskException) {
                throw e;
            }
            if (autoRetryRecorder.isCanRetry()) {
                retryTask();
            } else {
                throw e;
            }
        }
    }

    private void retryTask() throws Throwable {
        //回调重试
        onRetry();
        //决定是否延迟执行重试
        if (autoRetryRecorder.sleep()) {
            throw new CancelTaskException();
        }
        runTask();
    }

    /**
     * 尝试是否能重试任务
     * @param code
     * @param error
     * @throws Exception
     */
    protected final void tryToRetry(int code, String error) throws Exception {
        if (cancelAtomic.get()) {
            throw new CancelTaskException();
        } else {
            if (autoRetryRecorder.isCanRetry()) {
                //自动重试下载
                throw new RetryTaskException();
            } else {
                throw new HttpErrorException(code, error);
            }
        }
    }

    @Override
    public final void run() {
        try {
            runTask();
        } catch (Throwable e) {
            if (e instanceof CancelTaskException) {
                onCancel();
            } else {
                onError(e);
            }
        }
        completeRun();
    }

    protected void completeRun() {
    }
}
