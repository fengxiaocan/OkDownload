package com.ok.request.core;

import com.ok.request.base.Execute;
import com.ok.request.base.IExecuteQueue;
import com.ok.request.config.Config;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.factory.ThreadTaskFactory;
import com.ok.request.listener.OnExecuteListener;
import com.ok.request.listener.OnExecuteQueueListener;

import java.util.ArrayList;
import java.util.List;

public class OkExecuteQueue extends OkExecute implements IExecuteQueue {
    protected List<Execute> queue;
    protected int maxExecuteTaskCount = Config.config().getMaxExecuteTaskCount();
    protected OnExecuteQueueListener executeQueueListener;

    protected OkExecuteQueue(List<Execute> queue) {
        super(null);
        this.queue = queue != null ? queue : new ArrayList<Execute>();
    }

    protected OkExecuteQueue() {
        super(null);
        this.queue = new ArrayList<>();
    }

    public static OkExecuteQueue with(List<Execute> queue) {
        return new OkExecuteQueue(queue);
    }

    public static OkExecuteQueue create() {
        return new OkExecuteQueue(new ArrayList<Execute>());
    }

    @Override
    public OkExecuteQueue addRequest(Execute runnable) {
        queue.add(runnable);
        return this;
    }

    @Override
    public Dispatcher start() {
        return ThreadTaskFactory.createExecuteQueue(this);
    }

    @Override
    public OkExecuteQueue setUseAutoRetry(boolean useAutoRetry) {
        super.setUseAutoRetry(useAutoRetry);
        return this;
    }

    @Override
    public OkExecuteQueue setAutoRetryTimes(int autoRetryTimes) {
        super.setAutoRetryTimes(autoRetryTimes);
        return this;
    }

    @Override
    public OkExecuteQueue setAutoRetryInterval(int autoRetryInterval) {
        super.setAutoRetryInterval(autoRetryInterval);
        return this;
    }

    @Override
    public OkExecuteQueue scheduleOn(Schedulers schedulers) {
        super.scheduleOn(schedulers);
        return this;
    }

    @Override
    public OkExecuteQueue setOnExecuteListener(OnExecuteListener executeListener) {
        super.setOnExecuteListener(executeListener);
        return this;
    }

    @Override
    public final IExecuteQueue then(IExecuteQueue thenQueue) {
        return new OkExecuteRequestQueues(this).then(thenQueue);
    }

    @Override
    public final IExecuteQueue then(Execute runnable) {
        return then(cloneData(OkExecuteQueue.create()).addRequest(runnable));
    }

    @Override
    public final IExecuteQueue then(List<Execute> runnables) {
        return then(cloneData(OkExecuteQueue.with(runnables)));
    }

    public OnExecuteQueueListener getExecuteQueueListener() {
        return executeQueueListener;
    }

    @Override
    public OkExecuteQueue setExecuteQueueListener(OnExecuteQueueListener executeQueueListener) {
        this.executeQueueListener = executeQueueListener;
        return this;
    }

    OkExecuteQueue cloneData(IExecuteQueue queue) {
        queue.scheduleOn(schedulers);
        queue.setUseAutoRetry(isUseAutoRetry);
        queue.setAutoRetryTimes(autoRetryTimes);
        queue.setAutoRetryInterval(autoRetryInterval);
        queue.setAutoRetryInterval(autoRetryInterval);
        queue.setMaxExecuteTaskCount(maxExecuteTaskCount);
        return (OkExecuteQueue) queue;
    }

    public int getMaxExecuteTaskCount() {
        return maxExecuteTaskCount;
    }

    /**
     * 设置任务运行的最大线程数
     *
     * @param maxExecuteTaskCount
     * @return
     */
    @Override
    public OkExecuteQueue setMaxExecuteTaskCount(int maxExecuteTaskCount) {
        this.maxExecuteTaskCount = maxExecuteTaskCount;
        return this;
    }

    public List<OkExecute> cloneToExecute() {
        List<OkExecute> list = new ArrayList<>();
        for (Execute runnable : queue) {
            OkExecute request = new OkExecute(runnable);
            request.schedulers = schedulers;
            request.isUseAutoRetry = isUseAutoRetry;
            request.autoRetryTimes = autoRetryTimes;
            request.autoRetryInterval = autoRetryInterval;
            request.executeListener = executeListener;
            list.add(request);
        }
        return list;
    }
}
