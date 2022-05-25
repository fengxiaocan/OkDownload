package com.x.down.task;


import com.x.down.ExecutorGather;
import com.x.down.XDownload;
import com.x.down.base.IConnectRequest;
import com.x.down.core.XExecuteRequest;
import com.x.down.core.XExecuteRequestQueue;
import com.x.down.core.XExecuteRequestQueues;
import com.x.down.impl.ExecuteQueueDisposer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

final class ExecuteQueueTask implements Runnable, IConnectRequest {
    private final XExecuteRequestQueue requestQueue;
    private final XExecuteRequestQueues thenQueue;
    private volatile boolean isCancel = false;
    private volatile Future taskFuture;

    public ExecuteQueueTask(XExecuteRequestQueue requestQueue) {
        this.requestQueue = requestQueue;
        this.thenQueue = null;
    }

    public ExecuteQueueTask(XExecuteRequestQueues thenQueue) {
        this.requestQueue = null;
        this.thenQueue = thenQueue;
    }

    public final void setTaskFuture(Future taskFuture) {
        this.taskFuture = taskFuture;
    }

    @Override
    public void run() {
        if (isCancel) return;

        if (requestQueue != null) {
            runQueueTask(requestQueue, requestQueue.getTag());
        }

        while (!isCancel && thenQueue != null && thenQueue.checkSize()) {
            //开启下一轮任务队列
            XExecuteRequestQueue pollFirst = thenQueue.pollFirst();
            runQueueTask(pollFirst, thenQueue.getTag());
        }
        removeRequest();
        taskFuture = null;
    }

    //执行队列任务请求
    private void runQueueTask(XExecuteRequestQueue queue, String queueTag) {
        //创建线程池
        ThreadPoolExecutor poolExecutor = ExecutorGather.newSubTaskQueue(queue.getMaxExecuteTaskCount());
        //获取任务栈
        List<XExecuteRequest> requests = queue.cloneToExecute();
        //创建计数器
        final CountDownLatch countDownLatch = new CountDownLatch(requests.size());
        //创建任务计数处理器
        final ExecuteQueueDisposer disposer = new ExecuteQueueDisposer(queue, countDownLatch, requests.size());

        synchronized (Object.class) {
            for (XExecuteRequest request : requests) {
                ExecuteRequestTask requestTask = new ExecuteRequestTask(request, disposer);
                Future<?> submit = poolExecutor.submit(requestTask);
                XDownload.get().addExecuteRequest(queueTag, requestTask);
                requestTask.setTaskFuture(submit);
            }
        }
        //等待下载完成
        try {
            //阻塞
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //清除标记,防止内存泄漏
        XDownload.get().removeExecuteRequest(queueTag);
        //线程池停止回收
        poolExecutor.shutdown();
    }

    //清除标记,防止内存泄漏
    private void removeRequest() {
        if (requestQueue != null) {
            XDownload.get().removeExecuteRequest(requestQueue.getTag());
        }
        if (thenQueue != null) {
            XDownload.get().removeExecuteRequest(thenQueue.getTag());
            XDownload.get().removeRequest(thenQueue.getTag());
        }
    }

    @Override
    public boolean cancel() {
        isCancel = true;
        if (taskFuture != null) {
            return taskFuture.cancel(true);
        }
        return false;
    }

}
