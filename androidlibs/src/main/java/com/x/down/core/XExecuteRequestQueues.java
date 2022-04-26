package com.x.down.core;

import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnExecuteListener;
import com.x.down.listener.OnExecuteQueueListener;
import com.x.down.task.ThreadTaskFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class XExecuteRequestQueues implements IExecuteQueue {
    private final String tag = UUID.randomUUID().toString();//标记
    private final LinkedList<IExecuteQueue> requestQueues;

    protected XExecuteRequestQueues(IExecuteQueue queue) {
        this.requestQueues = new LinkedList<>();
        this.requestQueues.add(queue);
    }

    @Override
    public IExecuteQueue addRequest(Execute runnable) {
        getLast().addRequest(runnable);
        return this;
    }

    @Override
    public IExecuteQueue setMaxExecuteTaskCount(int maxExecuteTaskCount) {
        getLast().setMaxExecuteTaskCount(maxExecuteTaskCount);
        return this;
    }

    @Override
    public IExecuteQueue setExecuteQueueListener(OnExecuteQueueListener executeQueueListener) {
        getLast().setExecuteQueueListener(executeQueueListener);
        return this;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public IExecuteRequest setTag(String tag) {
        getLast().setTag(tag);
        return this;
    }

    @Override
    public IExecuteRequest setUseAutoRetry(boolean useAutoRetry) {
        getLast().setUseAutoRetry(useAutoRetry);
        return this;
    }

    @Override
    public IExecuteRequest setAutoRetryTimes(int autoRetryTimes) {
        getLast().setAutoRetryTimes(autoRetryTimes);
        return this;
    }

    @Override
    public IExecuteRequest setAutoRetryInterval(int autoRetryInterval) {
        getLast().setAutoRetryInterval(autoRetryInterval);
        return this;
    }

    @Override
    public IExecuteRequest setSchedulers(Schedulers schedulers) {
        getLast().setSchedulers(schedulers);
        return this;
    }

    @Override
    public IExecuteRequest setExecuteListener(OnExecuteListener executeListener) {
        getLast().setExecuteListener(executeListener);
        return this;
    }

    @Override
    public IExecuteQueue then(IExecuteQueue thenQueue) {
        requestQueues.add(thenQueue);
        return this;
    }

    @Override
    public IExecuteQueue then(Execute runnable) {
        XExecuteRequestQueue queue = getLast().cloneData(XExecuteRequestQueue.create());
        requestQueues.add(queue.addRequest(runnable));
        return this;
    }

    @Override
    public IExecuteQueue then(List<Execute> runnables) {
        XExecuteRequestQueue with = XExecuteRequestQueue.with(runnables);
        requestQueues.add(getLast().cloneData(with));
        return this;
    }

    @Override
    public String start() {
        ThreadTaskFactory.createExecuteRequestQueues(this);
        return tag;
    }

    public XExecuteRequestQueue pollFirst() {
        return (XExecuteRequestQueue) requestQueues.pollFirst();
    }

    public XExecuteRequestQueue getLast() {
        return (XExecuteRequestQueue) requestQueues.getLast();
    }

    public boolean checkSize() {
        return requestQueues.size() > 0;
    }
}
