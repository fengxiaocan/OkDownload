package com.x.down.impl;

import com.x.down.core.XExecuteRequestQueue;
import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnExecuteQueueListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExecuteQueueDisposer {

    protected final Schedulers schedulers;
    private final CountDownLatch countDownLatch;
    private final int taskCount;//总任务数
    private final OnExecuteQueueListener executeQueueListener;
    private AtomicInteger completeIndex = new AtomicInteger(0);//执行完成位置
    private AtomicInteger counting = new AtomicInteger(0);//执行到的位置

    public ExecuteQueueDisposer(XExecuteRequestQueue queue, CountDownLatch countDownLatch, int taskCount) {
        this.executeQueueListener = queue.getExecuteQueueListener();
        this.schedulers = queue.getSchedulers();
        this.taskCount = taskCount;
        this.countDownLatch = countDownLatch;
    }

    public synchronized void onCancel() {
        counting.getAndIncrement();
        extracted();
        countDownLatch.countDown();
    }

    public synchronized void onError() {
        counting.getAndIncrement();
        extracted();
        countDownLatch.countDown();
    }

    public synchronized void onComplete() {
        counting.getAndIncrement();
        completeIndex.getAndIncrement();
        extracted();
        countDownLatch.countDown();
    }

    private synchronized void extracted() {
        if (counting.get() >= taskCount && executeQueueListener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        executeQueueListener.onComplete(taskCount, completeIndex.get());
                    }
                });
            } else {
                executeQueueListener.onComplete(taskCount, completeIndex.get());
            }
        }
    }

}
