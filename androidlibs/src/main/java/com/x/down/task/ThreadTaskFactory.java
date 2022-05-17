package com.x.down.task;

import com.x.down.ExecutorGather;
import com.x.down.XDownload;
import com.x.down.core.XDownloadRequest;
import com.x.down.core.XExecuteRequest;
import com.x.down.core.XExecuteRequestQueue;
import com.x.down.core.XExecuteRequestQueues;
import com.x.down.core.XHttpRequest;
import com.x.down.core.XHttpRequestQueue;
import com.x.down.impl.DownloadListenerDisposer;

import java.util.List;
import java.util.concurrent.Future;

public final class ThreadTaskFactory {
    public static void createSingleDownloadTask(XDownloadRequest request) {
        final DownloadListenerDisposer disposer = new DownloadListenerDisposer(request);
        SingleDownloadThreadTask requestTask = new SingleDownloadThreadTask(request, disposer, 0);
        Future future = ExecutorGather.executorDownloaderQueue().submit(requestTask);
        XDownload.get().addDownload(request.getTag(), requestTask);
        requestTask.setTaskFuture(future);
    }

    public static void createDownloadThreadRequest(XDownloadRequest request) {
        DownloadThreadRequest requestTask = new DownloadThreadRequest(request);
        Future future = ExecutorGather.executorDownloaderQueue().submit(requestTask);
        XDownload.get().addDownload(request.getTag(), requestTask);
        requestTask.setTaskFuture(future);
    }

    public static void createM3u8DownloaderRequest(XDownloadRequest request) {
        M3u8DownloaderRequest requestTask = new M3u8DownloaderRequest(request);
        Future future = ExecutorGather.executorDownloaderQueue().submit(requestTask);
        XDownload.get().addDownload(request.getTag(), requestTask);
        requestTask.setTaskFuture(future);
    }

    /**
     * 创建http请求任务队列
     *
     * @param request
     */
    public static void createHttpRequestTask(XHttpRequest request) {
        HttpRequestTask requestTask = new HttpRequestTask(request);
        Future future = ExecutorGather.executorTaskQueue().submit(requestTask);
        XDownload.get().addRequest(request.getTag(), requestTask);
        requestTask.setTaskFuture(future);
    }

    /**
     * 创建http请求任务
     *
     * @param request
     */
    public static void createHttpRequestTaskQueue(XHttpRequestQueue request) {
        List<XHttpRequest> requests = request.cloneToRequest();
        for (XHttpRequest httpRequest : requests) {
            createHttpRequestTask(httpRequest);
        }
    }

    /**
     * 创建执行多线程任务请求
     *
     * @param request
     */
    public static void createExecuteRequest(XExecuteRequest request) {
        ExecuteRequestTask requestTask = new ExecuteRequestTask(request);
        Future future = ExecutorGather.executorTaskQueue().submit(requestTask);
        XDownload.get().addExecuteRequest(request.getTag(), requestTask);
        requestTask.setTaskFuture(future);
    }

    /**
     * 创建执行多线程任务队列请求
     *
     * @param queue
     */
    public static void createExecuteRequestQueue(XExecuteRequestQueue queue) {
        ExecuteQueueTask queueTask = new ExecuteQueueTask(queue);
        Future future = ExecutorGather.executorTaskQueue().submit(queueTask);
        XDownload.get().addExecuteRequest(queue.getTag(), queueTask);
        queueTask.setTaskFuture(future);
    }

    /**
     * 创建执行多线程任务队列请求
     *
     * @param queues
     */
    public static void createExecuteRequestQueues(XExecuteRequestQueues queues) {
        ExecuteQueueTask queueTask = new ExecuteQueueTask(queues);
        Future future = ExecutorGather.executorTaskQueue().submit(queueTask);
        XDownload.get().addRequest(queues.getTag(), queueTask);
        queueTask.setTaskFuture(future);
    }

}
