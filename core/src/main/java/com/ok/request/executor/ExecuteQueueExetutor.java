package com.ok.request.executor;

import com.ok.request.CoreDownload;
import com.ok.request.core.OkExecute;
import com.ok.request.core.OkExecuteQueue;
import com.ok.request.core.OkExecuteRequestQueues;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.disposer.ExecuteQueueDisposer;
import com.ok.request.exception.CancelTaskException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

public class ExecuteQueueExetutor extends AutoRetryExecutor {
    private final OkExecuteQueue requestQueue;
    private final OkExecuteRequestQueues thenQueue;

    public ExecuteQueueExetutor(OkExecuteQueue requestQueue) {
        super(requestQueue);
        CoreDownload.addExecute(requestQueue.getTag(), this);
        this.requestQueue = requestQueue;
        this.thenQueue = null;
    }

    public ExecuteQueueExetutor(OkExecuteRequestQueues thenQueue) {
        super(thenQueue);
        CoreDownload.addExecute(thenQueue.getTag(), this);
        this.requestQueue = null;
        this.thenQueue = thenQueue;
    }

    @Override
    protected void onExecute() throws Throwable {
        if (requestQueue != null) {
            runQueueTask(requestQueue);
        }
        while (thenQueue != null && thenQueue.checkSize()) {
            checkIsCancel();
            //开启下一轮任务队列
            OkExecuteQueue pollFirst = thenQueue.pollFirst();
            runQueueTask(pollFirst);
        }
    }

    //执行队列任务请求
    private void runQueueTask(OkExecuteQueue queue) {
        checkIsCancel();
        //创建线程池
        ThreadPoolExecutor poolExecutor = CoreDownload.newSubTaskQueue(queue.getMaxExecuteTaskCount());
        //获取任务栈
        List<OkExecute> requests = queue.cloneToExecute();
        //创建计数器
        final CountDownLatch countDownLatch = new CountDownLatch(requests.size());
        //创建任务计数处理器
        final ExecuteQueueDisposer disposer = new ExecuteQueueDisposer(queue, countDownLatch, requests.size());
        final List<Dispatcher> dispatcherList = new ArrayList<>();
        try {
            synchronized (Object.class) {
                for (OkExecute request : requests) {
                    checkIsCancel();
                    ExecuteRequestExecutor requestTask = new ExecuteRequestExecutor(request, disposer);
                    dispatcherList.add(requestTask);
                    requestTask.setTaskFuture(poolExecutor.submit(requestTask));
                }
            }
            //阻塞等待任务完成
            countDownLatch.await();
        } catch (InterruptedException e) {
            for (Dispatcher dispatcher : dispatcherList) {
                dispatcher.cancel();
            }
            throw new CancelTaskException();
        }finally {
            //线程池停止回收
            poolExecutor.shutdownNow();
        }
    }

    //清除标记,防止内存泄漏
    private void removeRequest() {
        CoreDownload.removeExecute(requestQueue.getTag(), this);
        CoreDownload.removeExecute(thenQueue.getTag(), this);
    }

    @Override
    protected void completeRun() {
        removeRequest();
    }

    @Override
    protected void applyCancel() {

    }
}
