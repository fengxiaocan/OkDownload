package com.ok.request.disposer;

import com.ok.request.core.OkExecuteQueue;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.listener.OnExecuteQueueListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExecuteQueueDisposer {
    protected final Schedulers schedulers;
    private final CountDownLatch countDownLatch;
    private final int taskCount;//总任务数
    private final OnExecuteQueueListener executeQueueListener;
    private AtomicInteger completeIndex = new AtomicInteger(0);//执行完成位置
    private AtomicInteger counting = new AtomicInteger(0);//执行到的位置

    public ExecuteQueueDisposer(OkExecuteQueue queue, CountDownLatch countDownLatch, int taskCount) {
        this.executeQueueListener = queue.getExecuteQueueListener();
        this.schedulers = queue.schedulers();
        this.taskCount = taskCount;
        this.countDownLatch = countDownLatch;
    }

    public synchronized void onCancel() {
        counting.getAndIncrement();
    }

    public synchronized void onError() {
        counting.getAndIncrement();
    }

    public synchronized void onComplete() {
        counting.getAndIncrement();
        completeIndex.getAndIncrement();
    }

    public synchronized void onFinish() {
        countDownLatch.countDown();
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
