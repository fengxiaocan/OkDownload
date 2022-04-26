package com.x.down.core;

import com.x.down.config.Config;
import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnExecuteListener;
import com.x.down.listener.OnExecuteQueueListener;
import com.x.down.task.ThreadTaskFactory;

import java.util.ArrayList;
import java.util.List;

public class XExecuteRequestQueue extends XExecuteRequest implements IExecuteQueue {
    protected List<Execute> queue;
    protected int maxExecuteTaskCount = Config.config().getRequestMaxTaskCount();
    protected OnExecuteQueueListener executeQueueListener;

    protected XExecuteRequestQueue(List<Execute> queue) {
        super(null);
        this.queue = queue != null ? queue : new ArrayList<Execute>();
    }

    protected XExecuteRequestQueue() {
        super(null);
        this.queue = new ArrayList<>();
    }

    public static XExecuteRequestQueue with(List<Execute> queue) {
        return new XExecuteRequestQueue(queue);
    }

    public static XExecuteRequestQueue create() {
        return new XExecuteRequestQueue(new ArrayList<Execute>());
    }

    @Override
    public XExecuteRequestQueue addRequest(Execute runnable) {
        queue.add(runnable);
        return this;
    }

    @Override
    public String start() {
        ThreadTaskFactory.createExecuteRequestQueue(this);
        return getTag();
    }

    @Override
    public XExecuteRequestQueue setUseAutoRetry(boolean useAutoRetry) {
        super.setUseAutoRetry(useAutoRetry);
        return this;
    }

    @Override
    public XExecuteRequestQueue setAutoRetryTimes(int autoRetryTimes) {
        super.setAutoRetryTimes(autoRetryTimes);
        return this;
    }

    @Override
    public XExecuteRequestQueue setAutoRetryInterval(int autoRetryInterval) {
        super.setAutoRetryInterval(autoRetryInterval);
        return this;
    }

    @Override
    public XExecuteRequestQueue setSchedulers(Schedulers schedulers) {
        super.setSchedulers(schedulers);
        return this;
    }

    @Override
    public XExecuteRequestQueue setExecuteListener(OnExecuteListener executeListener) {
        super.setExecuteListener(executeListener);
        return this;
    }

    @Override
    public final IExecuteQueue then(IExecuteQueue thenQueue) {
        return new XExecuteRequestQueues(this).then(thenQueue);
    }

    @Override
    public final IExecuteQueue then(Execute runnable) {
        return then(cloneData(XExecuteRequestQueue.create()).addRequest(runnable));
    }

    @Override
    public final IExecuteQueue then(List<Execute> runnables) {
        return then(cloneData(XExecuteRequestQueue.with(runnables)));
    }

    public OnExecuteQueueListener getExecuteQueueListener() {
        return executeQueueListener;
    }

    @Override
    public XExecuteRequestQueue setExecuteQueueListener(OnExecuteQueueListener executeQueueListener) {
        this.executeQueueListener = executeQueueListener;
        return this;
    }

    XExecuteRequestQueue cloneData(IExecuteQueue queue) {
        queue.setSchedulers(schedulers);
        queue.setUseAutoRetry(isUseAutoRetry);
        queue.setAutoRetryTimes(autoRetryTimes);
        queue.setAutoRetryInterval(autoRetryInterval);
        queue.setAutoRetryInterval(autoRetryInterval);
        queue.setMaxExecuteTaskCount(maxExecuteTaskCount);
        return (XExecuteRequestQueue) queue;
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
    public XExecuteRequestQueue setMaxExecuteTaskCount(int maxExecuteTaskCount) {
        this.maxExecuteTaskCount = maxExecuteTaskCount;
        return this;
    }

    public List<XExecuteRequest> cloneToExecute() {
        List<XExecuteRequest> list = new ArrayList<>();
        for (Execute runnable : queue) {
            XExecuteRequest request = new XExecuteRequest(runnable);
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
