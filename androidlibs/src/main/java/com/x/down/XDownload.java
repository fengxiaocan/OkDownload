package com.x.down;

import android.content.Context;
import android.os.Build;

import com.x.down.base.IConnectRequest;
import com.x.down.config.Config;
import com.x.down.config.IConfig;
import com.x.down.config.XConfig;
import com.x.down.core.Execute;
import com.x.down.core.HttpConnect;
import com.x.down.core.HttpDownload;
import com.x.down.core.IExecuteRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.core.XExecuteRequest;
import com.x.down.core.XExecuteRequestQueue;
import com.x.down.core.XHttpRequest;
import com.x.down.core.XHttpRequestQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public final class XDownload {
    private static XDownload xDownload;
    private final Map<String, IConnectRequest> connectMap = new HashMap<>();
    private final Map<String, List<IConnectRequest>> downloadMap = new HashMap<>();

    private XDownload() {
    }

    public static IConfig init(Context context, String dirName) {
        XConfig config = new XConfig(context.getExternalCacheDir().getAbsolutePath());
        config.saveDir(context.getExternalFilesDir(dirName).getAbsolutePath());
        config.userAgent(getDefaultUserAgent());
        XDownload.get().config(config);
        return config;
    }

    public static IConfig init(Context context) {
        return init(context, "xdownload");
    }

    public static synchronized XDownload get() {
        if (xDownload == null) {
            xDownload = new XDownload();
        }
        return xDownload;
    }

    /**
     * 创建一个http请求
     *
     * @param baseUrl
     * @return
     */
    public static HttpConnect request(String baseUrl) {
        return XHttpRequest.with(baseUrl);
    }

    /**
     * 创建http请求队列
     *
     * @param queue
     * @return
     */
    public static HttpConnect requests(List<String> queue) {
        return XHttpRequestQueue.with(queue);
    }

    /**
     * 创建http请求队列
     *
     * @return
     */
    public static HttpConnect requests() {
        return XHttpRequestQueue.create();
    }

    /**
     * 创建一个下载任务
     *
     * @param baseUrl
     * @return
     */
    public static HttpDownload download(String baseUrl) {
        return XDownloadRequest.with(baseUrl);
    }


    /**
     * 创建一个任务请求
     *
     * @param runnable
     * @return
     */
    public static IExecuteRequest execute(Execute runnable) {
        return XExecuteRequest.with(runnable);
    }

    /**
     * 创建任务请求队列
     *
     * @param runnable
     * @return
     */
    public static XExecuteRequestQueue executes(Execute runnable) {
        return XExecuteRequestQueue.create().addRequest(runnable);
    }

    /**
     * 创建任务请求队列
     *
     * @param queue
     * @return
     */
    public static XExecuteRequestQueue executes(List<Execute> queue) {
        return XExecuteRequestQueue.with(queue);
    }

    /**
     * 创建任务请求队列
     *
     * @return
     */
    public static XExecuteRequestQueue executes() {
        return XExecuteRequestQueue.create();
    }

    public static String getDefaultUserAgent() {
        StringBuilder result = new StringBuilder("Mozilla/5.0 (");
        result.append(System.getProperty("os.name"));
        result.append("; Android ");
        result.append(Build.VERSION.RELEASE);
        result.append("; ");
        result.append(Build.MANUFACTURER);
        result.append(" ");
        if ("REL".equals(Build.VERSION.CODENAME)) {
            String model = Build.MODEL;
            if (model.length() > 0) {
                result.append(model);
            }
        }
        result.append("; ");
        String id = Build.ID;
        if (id.length() > 0) {
            result.append("Build/");
            result.append(id);
        }
        result.append(")");
        return result.toString();
    }

    public static ThreadPoolExecutor executorHttpQueue() {
        return ExecutorGather.executorHttpQueue();
    }

    /**
     * 创建多线程下载的子任务线程池队列
     */
    public static ThreadPoolExecutor newSubTaskQueue(int corePoolSize) {
        return ExecutorGather.newSubTaskQueue(corePoolSize);
    }

    /**
     * 创建下载的线程队列
     *
     * @return
     */
    public static ThreadPoolExecutor executorDownloaderQueue() {
        return ExecutorGather.executorDownloaderQueue();
    }

    public static synchronized void recyclerDownloaderQueue() {
        ExecutorGather.recyclerDownloaderQueue();
    }

    public static synchronized void recyclerHttpQueue() {
        ExecutorGather.recyclerHttpQueue();
    }

    public static synchronized void recyclerSingleQueue() {
        ExecutorGather.recyclerSingleQueue();
    }

    public static synchronized void recyclerAllQueue() {
        ExecutorGather.recyclerAllQueue();
    }

    public XDownload config(XConfig setting) {
        Config.initSetting(setting);
        return this;
    }

    public synchronized XConfig config() {
        return Config.config();
    }

    public synchronized void addRequest(String tag, IConnectRequest connect) {
        if (tag == null) return;
        connectMap.put(tag, connect);
    }

    public synchronized IConnectRequest removeRequest(String tag) {
        if (tag == null) return null;
        return connectMap.remove(tag);
    }

    public synchronized void addDownload(String tag, IConnectRequest download) {
        if (tag == null) return;
        List<IConnectRequest> requestList = downloadMap.get(tag);
        if (requestList != null) {
            requestList.add(download);
        } else {
            requestList = new ArrayList<>();
            requestList.add(download);
            downloadMap.put(tag, requestList);
        }
    }

    public synchronized List<IConnectRequest> removeDownload(String tag) {
        if (tag == null) return null;
        return downloadMap.remove(tag);
    }

    /**
     * 取消请求
     *
     * @param tag
     * @return
     */
    public synchronized boolean cancleRequest(String tag) {
        if (tag == null) return false;
        IConnectRequest request = connectMap.remove(tag);
        if (request != null) {
            return request.cancel();
        }
        return false;
    }

    /**
     * 取消下载
     *
     * @param tag
     * @return
     */
    public synchronized boolean cancleDownload(String tag) {
        if (tag == null) return false;
        List<IConnectRequest> list = downloadMap.remove(tag);
        if (list != null) {
            boolean isCancel = false;
            for (IConnectRequest request : list) {
                boolean cancel = request.cancel();
                if (!isCancel) {
                    isCancel = cancel;
                }
            }
            return isCancel;
        }
        return false;
    }

    public synchronized void addExecuteRequest(String tag, IConnectRequest execute) {
        addDownload(tag, execute);
    }

    public synchronized List<IConnectRequest> removeExecuteRequest(String tag) {
        return removeDownload(tag);
    }

    /**
     * 取消对应标记的任务
     *
     * @param tag
     * @return
     */
    public synchronized boolean cancleExecuteRequest(String tag) {
        return cancleRequest(tag) | cancleDownload(tag);
    }

    /**
     * 清理所有的任务请求标记
     */
    public synchronized void clearAllRequest() {
        connectMap.clear();
        downloadMap.clear();
    }

    /**
     * 检测是否有任务请求
     *
     * @param tag
     * @return
     */
    public boolean checkRequest(String tag) {
        return connectMap.containsKey(tag) | downloadMap.containsKey(tag);
    }
}
