package com.x.down.impl;

import com.x.down.core.XExecuteRequestQueue;
import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnExecuteQueueListener;

import java.util.concurrent.CountDownLatch;

public final class ExecuteQueueDisposer {

    protected final Schedulers schedulers;
    private final CountDownLatch countDownLatch;
    private final int taskCount;//总任务数
    private final OnExecuteQueueListener executeQueueListener;
    private volatile int completeIndex = 0;//执行完成位置
    private volatile int counting = 0;//执行到的位置

    public ExecuteQueueDisposer(XExecuteRequestQueue queue, CountDownLatch countDownLatch, int taskCount) {
        this.executeQueueListener = queue.getExecuteQueueListener();
        this.schedulers = queue.getSchedulers();
        this.taskCount = taskCount;
        this.countDownLatch = countDownLatch;
    }

    public synchronized void onCancel() {
        counting++;
        extracted();
        countDownLatch.countDown();
    }

    public synchronized void onError() {
        counting++;
        extracted();
        countDownLatch.countDown();
    }

    public synchronized void onComplete() {
        counting++;
        completeIndex++;
        extracted();
        countDownLatch.countDown();
    }

    private synchronized void extracted() {
        if (counting >= taskCount && executeQueueListener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        executeQueueListener.onComplete(taskCount, completeIndex);
                    }
                });
            } else {
                executeQueueListener.onComplete(taskCount, completeIndex);
            }
        }
    }

}
