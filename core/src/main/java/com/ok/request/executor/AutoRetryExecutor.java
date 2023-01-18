package com.ok.request.executor;


import com.ok.request.base.Constants;
import com.ok.request.base.Executor;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.dispatch.OnDispatcher;
import com.ok.request.exception.CancelTaskException;
import com.ok.request.exception.RetryTaskException;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AutoRetryExecutor extends BaseExecute implements Runnable, Executor, Dispatcher {
    private final AtomicBoolean cancelAtomic = new AtomicBoolean(false);
    protected volatile int RUN_STATE;
    private Future taskFuture;
    private volatile String threadName;
    private volatile long threadId = -1;

    public AutoRetryExecutor(OnDispatcher onDispatcher) {
        super(onDispatcher);
        this.RUN_STATE = Constants.READY_STATE;
    }

    public final void setTaskFuture(Future taskFuture) {
        this.taskFuture = taskFuture;
    }

    @Override
    public final void run() {
        this.RUN_STATE = Constants.RUNNING_STATE;
        threadName = Thread.currentThread().getName();
        threadId = Thread.currentThread().getId();

        onStart(this);
        try {
            while (runTask()) {

            }
        } catch (Throwable e) {
            if (e instanceof CancelTaskException) {
                onCancel();
            } else {
                onError(e);
            }
        } finally {
            this.RUN_STATE = Constants.TERMINATED_STATE;
            onComplete(this);
            completeRun();
        }
        taskFuture = null;
    }

    private boolean runTask() throws Throwable {
        try {
            //判断是否取消为最高优先级
            checkIsCancel();

            onExecute();
            return false;
        } catch (Throwable e) {
            if (e instanceof CancelTaskException) {
                throw e;
            }
            checkIsCancel();
            if (e instanceof RetryTaskException) {
                retryTask();
                return true;
            }
            if (recorder().isCanRetry()) {
                retryTask();
                return true;
            } else {
                throw e;
            }
        }
    }

    private void retryTask() {
        //回调重试
        onRetry(recorder().getRetryCount());
        //决定是否延迟执行重试
        if (recorder().sleep()) {
            throw new CancelTaskException();
        }
    }

    public final void checkIsCancel() {
        if (cancelAtomic.get()) {
            throw new CancelTaskException();
        }
    }

    protected void onRetry(int retryCount) {
        onRetry(this, retryCount);
    }

    protected void onError(Throwable e) {
        onError(this, e);
    }

    protected void onCancel() {
        onCancel(this);
    }

    protected abstract void onExecute() throws Throwable;

    protected abstract void completeRun();

    protected abstract void applyCancel();

    @Override
    public final Object tag() {
        return tags;
    }

    @Override
    public final Dispatcher call() {
        return this;
    }

    @Override
    public final long id() {
        return threadId;
    }

    @Override
    public final String name() {
        return threadName;
    }

    @Override
    public final void cancel() {
        cancelAtomic.getAndSet(true);
        try {
            applyCancel();
        } catch (Throwable e) {

        }
        if (taskFuture != null) taskFuture.cancel(true);
        taskFuture = null;
    }

    @Override
    public final int state() {
        return RUN_STATE;
    }

}
