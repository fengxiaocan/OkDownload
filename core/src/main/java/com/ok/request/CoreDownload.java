package com.ok.request;

import com.ok.request.base.Execute;
import com.ok.request.base.HttpConnect;
import com.ok.request.base.HttpDownload;
import com.ok.request.base.IExecuteQueue;
import com.ok.request.base.IExecuteRequest;
import com.ok.request.base.Logger;
import com.ok.request.config.Config;
import com.ok.request.config.XConfig;
import com.ok.request.core.LoopWork;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.core.OkExecute;
import com.ok.request.core.OkExecuteQueue;
import com.ok.request.core.OkHttpRequest;
import com.ok.request.core.OkHttpRequestQueue;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.tool.XDownUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoreDownload extends ExecutorGather {
    private static final Map<Object, TaskPool> poolMap = new HashMap<>();

    public static void config(XConfig setting) {
        Config.initSetting(setting);
    }

    public static XConfig config() {
        return Config.config();
    }

    public static LoopWork loopWork() {
        return LoopWork.get();
    }

    /**
     * 创建一个http请求
     *
     * @param baseUrl
     * @return
     */
    public static HttpConnect request(String baseUrl) {
        return OkHttpRequest.with(baseUrl);
    }

    /**
     * 创建http请求队列
     *
     * @param queue
     * @return
     */
    public static HttpConnect requests(List<String> queue) {
        return OkHttpRequestQueue.with(queue);
    }

    /**
     * 创建http请求队列
     *
     * @return
     */
    public static HttpConnect requests() {
        return OkHttpRequestQueue.create();
    }

    /**
     * 创建一个下载任务
     *
     * @param baseUrl
     * @return
     */
    public static HttpDownload download(String baseUrl) {
        return OkDownloadRequest.with(baseUrl);
    }

    /**
     * 创建一个m3u8下载任务
     *
     * @param baseUrl
     * @return
     */
    public static HttpDownload downM3u8(String baseUrl) {
        return OkDownloadRequest.with(baseUrl).asM3u8();
    }

    /**
     * 创建一个m3u8解析下载任务
     *
     * @param m3u8Info m3u8的信息
     * @return
     */
    public static HttpDownload parseM3u8Info(String m3u8Info) {
        return OkDownloadRequest.with("").parseM3u8Info(m3u8Info);
    }

    /**
     * 创建一个m3u8解析下载任务
     *
     * @param m3u8File m3u8的信息
     * @return
     */
    public static HttpDownload parseM3u8(File m3u8File) {
        return OkDownloadRequest.with("").parseM3u8(m3u8File);
    }

    /**
     * 创建一个m3u8解析下载任务
     *
     * @param m3u8File m3u8的信息
     * @return
     */
    public static HttpDownload parseM3u8Path(String m3u8File) {
        return OkDownloadRequest.with("").parseM3u8Path(m3u8File);
    }

    /**
     * 创建一个任务请求
     *
     * @param runnable
     * @return
     */
    public static IExecuteRequest execute(Execute runnable) {
        return OkExecute.with(runnable);
    }

    /**
     * 创建任务请求队列
     *
     * @param runnable
     * @return
     */
    public static IExecuteQueue executes(Execute runnable) {
        return OkExecuteQueue.create().addRequest(runnable);
    }

    /**
     * 创建任务请求队列
     *
     * @param queue
     * @return
     */
    public static IExecuteQueue executes(List<Execute> queue) {
        return OkExecuteQueue.with(queue);
    }

    /**
     * 创建任务请求队列
     *
     * @return
     */
    public static OkExecuteQueue executes() {
        return OkExecuteQueue.create();
    }

    public static synchronized void addExecute(Object tag, Dispatcher dispatcher) {
        if (tag == null) return;
        TaskPool pool = poolMap.get(tag);
        if (pool == null) {
            poolMap.put(tag, new TaskPool(dispatcher));
        } else {
            pool.addRequest(dispatcher);
        }
    }

    public static synchronized boolean removeExecute(Object tag, Dispatcher dispatcher) {
        if (tag == null) return false;
        TaskPool pool = poolMap.get(tag);
        if (pool != null) {
            if (pool.removeRequest(dispatcher)) {
                poolMap.remove(tag);
            }
        }
        return false;
    }

    /**
     * 取消下载
     *
     * @param tag
     * @return
     */
    public static synchronized int cancelExecute(Object tag) {
        if (tag == null) return 0;
        TaskPool pool = poolMap.get(tag);
        if (pool != null) {
            int cancelCount = 0;
            for (Dispatcher request : pool.getPool()) {
                request.cancel();
                cancelCount++;
            }
            poolMap.remove(tag);
            return cancelCount;
        }
        return 0;
    }


    /**
     * 清理所有的任务请求标记
     */
    public static synchronized void clearAllExecute() {
        poolMap.clear();
    }

    /**
     * 取消所有任务
     *
     * @return
     */
    public static synchronized int cancelAllExecute() {
        Collection<TaskPool> values = poolMap.values();
        int cancelCount = 0;
        for (TaskPool pool : values) {
            for (Dispatcher request : pool.getPool()) {
                request.cancel();
                cancelCount++;
            }
        }
        poolMap.clear();
        return cancelCount;
    }

    /**
     * 检测是否有任务请求
     *
     * @param tag
     * @return
     */
    public static boolean checkExecute(Object tag) {
        return poolMap.containsKey(tag);
    }

    /**
     * 检测是否有任务请求
     * @return
     */
    public static void checkExecute() {
        Logger.logE("poolMap=" + poolMap.size());
        for (Object key : poolMap.keySet()) {
            Logger.logE("poolMap="+key);
        }
    }

    public static void deleteCache() {
        String dir = Config.config().getRecordDir();
        XDownUtils.deleteDir(new File(dir));
        String cacheDir = Config.config().getCacheDir();
        XDownUtils.deleteDir(new File(cacheDir));
    }
}
