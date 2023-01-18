package com.ok.request.core;

import com.ok.request.base.Execute;
import com.ok.request.base.IExecuteQueue;
import com.ok.request.config.Config;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.dispatch.OnDispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.disposer.AutoRetryRecorder;
import com.ok.request.factory.ThreadTaskFactory;
import com.ok.request.listener.OnExecuteListener;
import com.ok.request.listener.OnExecuteQueueListener;

import java.util.LinkedList;
import java.util.List;

public final class OkExecuteRequestQueues implements IExecuteQueue, OnDispatcher {
    private Object tag = this.hashCode();
    private final LinkedList<IExecuteQueue> requestQueues;

    protected boolean isUseAutoRetry = Config.config().isUseAutoRetry();//是否使用出错自动重试
    protected int autoRetryTimes = Config.config().getAutoRetryTimes();//自动重试次数
    protected int autoRetryInterval = Config.config().getAutoRetryInterval();//自动重试间隔
    protected Schedulers schedulers;//调度器
    protected OnExecuteListener executeListener;


    protected OkExecuteRequestQueues(IExecuteQueue queue) {
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
        for (IExecuteQueue queue : requestQueues) {
            queue.setExecuteQueueListener(executeQueueListener);
        }
        return this;
    }

    @Override
    public Object getTag() {
        return tag;
    }

    @Override
    public IExecuteQueue setTag(Object tag) {
        this.tag = tag;
        return this;
    }

    @Override
    public IExecuteQueue then(IExecuteQueue thenQueue) {
        requestQueues.add(thenQueue);
        return this;
    }

    @Override
    public IExecuteQueue then(Execute runnable) {
        OkExecuteQueue queue = getLast().cloneData(OkExecuteQueue.create());
        requestQueues.add(queue.addRequest(runnable));
        return this;
    }

    @Override
    public IExecuteQueue then(List<Execute> runnables) {
        OkExecuteQueue with = OkExecuteQueue.with(runnables);
        requestQueues.add(getLast().cloneData(with));
        return this;
    }

    @Override
    public IExecuteQueue setUseAutoRetry(boolean useAutoRetry) {
        this.isUseAutoRetry = useAutoRetry;
        for (IExecuteQueue queue : requestQueues) {
            queue.setUseAutoRetry(useAutoRetry);
        }
        return this;
    }

    @Override
    public IExecuteQueue setAutoRetryTimes(int autoRetryTimes) {
        this.autoRetryTimes = autoRetryTimes;
        for (IExecuteQueue queue : requestQueues) {
            queue.setAutoRetryTimes(autoRetryTimes);
        }
        return this;
    }

    @Override
    public IExecuteQueue setAutoRetryInterval(int autoRetryInterval) {
        this.autoRetryInterval = autoRetryInterval;
        for (IExecuteQueue queue : requestQueues) {
            queue.setAutoRetryInterval(autoRetryInterval);
        }
        return this;
    }

    @Override
    public IExecuteQueue scheduleOn(Schedulers schedulers) {
        this.schedulers = schedulers;
        for (IExecuteQueue queue : requestQueues) {
            queue.scheduleOn(schedulers);
        }
        return this;
    }

    @Override
    public IExecuteQueue setExecuteListener(OnExecuteListener executeListener) {
        this.executeListener = executeListener;
        return this;
    }

    public OkExecuteQueue pollFirst() {
        return (OkExecuteQueue) requestQueues.pollFirst();
    }

    public OkExecuteQueue getLast() {
        return (OkExecuteQueue) requestQueues.getLast();
    }

    public boolean checkSize() {
        return requestQueues.size() > 0;
    }


    @Override
    public Dispatcher start() {
        return ThreadTaskFactory.createExecuteRequestQueues(this);
    }

    @Override
    public Schedulers schedulers() {
        return schedulers;
    }

    @Override
    public AutoRetryRecorder recorder() {
        return new AutoRetryRecorder(isUseAutoRetry, autoRetryTimes,autoRetryInterval);
    }

    @Override
    public OnExecuteListener executor() {
        return executeListener;
    }
}
