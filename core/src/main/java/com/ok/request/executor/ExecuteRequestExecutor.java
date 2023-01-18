package com.ok.request.executor;


import com.ok.request.CoreDownload;
import com.ok.request.core.OkExecute;
import com.ok.request.disposer.ExecuteQueueDisposer;

public class ExecuteRequestExecutor extends AutoRetryExecutor {
    protected final OkExecute xExecuteRequest;
    protected final ExecuteQueueDisposer queueDisposer;

    public ExecuteRequestExecutor(OkExecute request, ExecuteQueueDisposer disposer) {
        super(request);
        CoreDownload.addExecute(request.getTag(), this);
        this.xExecuteRequest = request;
        this.queueDisposer = disposer;
    }

    @Override
    protected void completeRun() {
        CoreDownload.removeExecute(tag(), this);
        if (queueDisposer != null) {
            queueDisposer.onFinish();
        }
    }

    @Override
    protected void applyCancel() {
    }

    @Override
    protected void onExecute() throws Throwable {
        xExecuteRequest.getExecute().run();
        if (queueDisposer != null) {
            queueDisposer.onComplete();
        }
    }

    @Override
    protected void onError(Throwable e) {
        super.onError(e);
        if (queueDisposer != null) {
            queueDisposer.onError();
        }
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        if (queueDisposer != null) {
            queueDisposer.onCancel();
        }
    }

}
