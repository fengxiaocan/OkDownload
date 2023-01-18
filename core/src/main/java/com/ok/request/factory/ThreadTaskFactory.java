package com.ok.request.factory;

import com.ok.request.CoreDownload;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.core.OkExecute;
import com.ok.request.core.OkExecuteQueue;
import com.ok.request.core.OkExecuteRequestQueues;
import com.ok.request.core.OkHttpRequest;
import com.ok.request.core.OkHttpRequestQueue;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.down.HttpDownloadExecutor;
import com.ok.request.executor.ExecuteQueueExetutor;
import com.ok.request.executor.ExecuteRequestExecutor;
import com.ok.request.executor.HttpExecutor;
import com.ok.request.m3u8.M3u8DownloadExecutor;

import java.util.ArrayList;
import java.util.List;

public final class ThreadTaskFactory {
    /**
     * 创建http请求任务队列
     *
     * @param request
     * @return
     */
    public static Dispatcher createHttpRequestTask(OkHttpRequest request) {
        HttpExecutor requestTask = new HttpExecutor(request);
        requestTask.setTaskFuture(CoreDownload.executorTaskQueue().submit(requestTask));
        return requestTask;
    }

    public static Dispatcher createDownloadThreadRequest(OkDownloadRequest request) {
        HttpDownloadExecutor requestTask = new HttpDownloadExecutor(request);
        requestTask.setTaskFuture(CoreDownload.executorDownloaderQueue().submit(requestTask));
        return requestTask;
    }

    public static Dispatcher createM3u8DownloaderRequest(OkDownloadRequest request) {
        M3u8DownloadExecutor requestTask = new M3u8DownloadExecutor(request);
        requestTask.setTaskFuture(CoreDownload.executorDownloaderQueue().submit(requestTask));
        return requestTask;
    }

    /**
     * 创建http请求任务
     *
     * @param request
     */
    public static List<Dispatcher> createHttpRequestTaskQueue(OkHttpRequestQueue request) {
        List<OkHttpRequest> requests = request.cloneToRequest();
        List<Dispatcher> dispatchers = new ArrayList<>();
        for (OkHttpRequest httpRequest : requests) {
            Dispatcher dispatcher = createHttpRequestTask(httpRequest);
            dispatchers.add(dispatcher);
        }
        return dispatchers;
    }

    /**
     * 创建执行多线程任务请求
     *
     * @param request
     */
    public static Dispatcher createExecuteRequest(OkExecute request) {
        ExecuteRequestExecutor requestTask = new ExecuteRequestExecutor(request,null);
        requestTask.setTaskFuture(CoreDownload.executorTaskQueue().submit(requestTask));
        return requestTask;
    }

    /**
     * 创建执行多线程任务队列请求
     *
     * @param queue
     */
    public static Dispatcher createExecuteQueue(OkExecuteQueue queue) {
        ExecuteQueueExetutor queueTask = new ExecuteQueueExetutor(queue);
        queueTask.setTaskFuture(CoreDownload.executorTaskQueue().submit(queueTask));
        return queueTask;
    }

    /**
     * 创建执行多线程任务队列请求
     *
     * @param queues
     */
    public static Dispatcher createExecuteRequestQueues(OkExecuteRequestQueues queues) {
        ExecuteQueueExetutor queueTask = new ExecuteQueueExetutor(queues);
        queueTask.setTaskFuture(CoreDownload.executorTaskQueue().submit(queueTask));
        return queueTask;
    }

}
