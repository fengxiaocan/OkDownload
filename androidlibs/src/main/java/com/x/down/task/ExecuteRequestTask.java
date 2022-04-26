package com.x.down.task;


import com.x.down.XDownload;
import com.x.down.base.IConnectRequest;
import com.x.down.base.IExecute;
import com.x.down.core.XExecuteRequest;
import com.x.down.impl.ExecuteListenerDisposer;
import com.x.down.impl.ExecuteQueueDisposer;
import com.x.down.made.AutoRetryRecorder;

import java.util.concurrent.Future;

class ExecuteRequestTask extends BaseExecuteRequest implements IExecute, IConnectRequest {
    protected final XExecuteRequest xExecuteRequest;
    protected final ExecuteListenerDisposer listenerDisposer;
    protected ExecuteQueueDisposer queueDisposer;
    protected Future taskFuture;

    public ExecuteRequestTask(XExecuteRequest request) {
        super(new AutoRetryRecorder(request.isUseAutoRetry(),
                request.getAutoRetryTimes(),
                request.getAutoRetryInterval()));
        this.listenerDisposer = new ExecuteListenerDisposer(request);
        this.xExecuteRequest = request;
        listenerDisposer.onPending(this);
    }

    public ExecuteRequestTask(XExecuteRequest request, ExecuteQueueDisposer disposer) {
        this(request);
        this.queueDisposer = disposer;
    }

    public final void setTaskFuture(Future taskFuture) {
        this.taskFuture = taskFuture;
    }

    @Override
    public void run() {
        listenerDisposer.onStart(this);
        super.run();
        XDownload.get().removeExecuteRequest(xExecuteRequest.getTag());
    }

    @Override
    protected void onExecute() throws Throwable {
        xExecuteRequest.getExecute().run();
        if (queueDisposer != null) {
            queueDisposer.onComplete();
        }
    }

    @Override
    protected void onRetry() {
        listenerDisposer.onRetry(this);
    }

    @Override
    protected void onError(Throwable e) {
        listenerDisposer.onError(this, e);
        if (queueDisposer != null) {
            queueDisposer.onError();
        }
    }

    @Override
    protected void onCancel() {
        listenerDisposer.onCancel(this);
        if (queueDisposer != null) {
            queueDisposer.onCancel();
        }
    }

    @Override
    public XExecuteRequest request() {
        return xExecuteRequest;
    }

    @Override
    public String tag() {
        return xExecuteRequest.getTag();
    }

    @Override
    public boolean cancel() {
        isCancel = true;
        if (taskFuture != null) {
            return taskFuture.cancel(true);
        }
        return false;
    }

    @Override
    public int retryCount() {
        return autoRetryRecorder.getRetryCount();
    }
}
