package com.ok.request.base;

import com.ok.request.call.Interceptor;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.listener.OnDownloadListener;
import com.ok.request.listener.OnExecuteListener;
import com.ok.request.listener.OnM3u8ParseIntercept;
import com.ok.request.listener.OnMergeM3u8Listener;
import com.ok.request.listener.OnProgressListener;
import com.ok.request.listener.OnSpeedListener;
import com.ok.request.listener.SSLCertificateFactory;
import com.ok.request.params.Headers;
import com.ok.request.params.Params;

import java.io.File;

public interface HttpDownload extends IConnect {
    /**
     * 设置文件的名称,非全路径,只是文件名字,默认会自动获取文件的文件名
     *
     * @param name 文件名字
     * @return
     */
    HttpDownload setFileName(String name);

    /**
     * 设置文件的保存文件夹路径,默认路径:JAVA在当前目录下,Android在沙盒文件夹中
     *
     * @param dir
     * @return
     */
    HttpDownload setFileDir(String dir);

    /**
     * 设置文件的保存文件夹路径,默认路径:JAVA在当前目录下,Android在沙盒文件夹中
     *
     * @param dir
     * @return
     */
    HttpDownload setFileDir(File dir);

    /**
     * 设置保存为文件的全路径
     *
     * @param saveFile 文件的全路径
     * @return
     */
    HttpDownload setSaveFile(String saveFile);

    /**
     * 设置保存为文件的全路径
     *
     * @param saveFile 文件的全路径
     * @return
     */
    HttpDownload setSaveFile(File saveFile);

    /**
     * 设置下载文件的缓存目录,默认路径:JAVA在当前目录下,Android在沙盒文件夹中
     *
     * @param cacheDir
     * @return
     */
    HttpDownload setCacheDir(String cacheDir);

    /**
     * 设置是否忽略下载的进度
     *
     * @param ignoredProgress
     * @return
     */
    HttpDownload setIgnoredProgress(boolean ignoredProgress);

    /**
     * 设置更新进度的时间间隔
     *
     * @param updateProgressTimes
     * @return
     */
    HttpDownload setUpdateProgressTimes(int updateProgressTimes);

    /**
     * 是否忽略下载速度
     *
     * @param ignoredSpeed
     * @return
     */
    HttpDownload setIgnoredSpeed(boolean ignoredSpeed);

    /**
     * 设置更新下载速度的时间间隔
     *
     * @param updateSpeedTimes
     * @return
     */
    HttpDownload setUpdateSpeedTimes(int updateSpeedTimes);

    /**
     * 是否使用多线程下载
     *
     * @param useMultiThread
     * @return
     */
    HttpDownload setUseMultiThread(boolean useMultiThread);

    /**
     * 写入文件buff大小，该数值大小不能小于2048，数值变小，下载速度会变慢
     *
     * @param bufferedSize
     * @return
     */
    HttpDownload setBufferedSize(int bufferedSize);

    /**
     * 设置单个任务下载的多线程的数量
     *
     * @param multiThreadCount
     * @return
     */
    HttpDownload setDownloadMultiThreadSize(int multiThreadCount);

    /**
     * 设置多线程下载分块的最大的上限值
     *
     * @param size
     * @return
     */
    HttpDownload setMaxDownloadBlockSize(int size);

    /**
     * 设置多线程下载分块的最大的下限值
     *
     * @param size
     * @return
     */
    HttpDownload setMinDownloadBlockSize(int size);

    /**
     * 设置是否使用断点续传下载
     *
     * @param useBreakpointResume
     * @return
     */
    HttpDownload setUseBreakpointResume(boolean useBreakpointResume);

    /**
     * 设置下载完成或失败监听回调
     *
     * @param listener
     * @return
     */
    HttpDownload setDownloadListener(OnDownloadListener listener);

    /**
     * 设置下载进度监听
     *
     * @param listener
     * @return
     */
    HttpDownload setOnProgressListener(OnProgressListener listener);

    /**
     * 设置下载速度监听
     *
     * @param listener
     * @return
     */
    HttpDownload setOnSpeedListener(OnSpeedListener listener);

    /**
     * 设置文件合并回调
     *
     * @param listener
     * @return
     */
    HttpDownload setOnMegerM3u8Listener(OnMergeM3u8Listener listener);

    HttpDownload setOnM3u8ParseIntercept(OnM3u8ParseIntercept listener);

    /**
     * 删除当前下载
     *
     * @return
     */
    HttpDownload delete();

    /**
     * 作为m3u8下载
     *
     * @return
     */
    HttpDownload asM3u8();

    /**
     * 解析m3u8数据然后下载
     *
     * @return
     */
    HttpDownload parseM3u8(File fileM3u8);

    /**
     * 解析m3u8数据然后下载
     *
     * @return
     */
    HttpDownload parseM3u8Path(String fileM3u8);

    /**
     * 解析m3u8数据然后下载
     *
     * @return
     */
    HttpDownload parseM3u8Info(String m3u8Info);

    @Override
    HttpDownload setTag(Object tag);

    @Override
    HttpDownload setSSLCertificate(String path);

    @Override
    HttpDownload setSSLCertificateFactory(SSLCertificateFactory factory);

    @Override
    HttpDownload addParams(String name, String value);

    @Override
    HttpDownload addHeader(String name, String value);

    @Override
    HttpDownload setParams(Params params);

    @Override
    HttpDownload setHeader(Headers header);

    @Override
    HttpDownload setUserAgent(String userAgent);

    @Override
    HttpDownload setTimeOut(int iOTimeOut);

    @Override
    HttpDownload setUseAutoRetry(boolean useAutoRetry);

    @Override
    HttpDownload setAutoRetryTimes(int autoRetryTimes);

    @Override
    HttpDownload setAutoRetryInterval(int autoRetryInterval);

    @Override
    HttpDownload permitAllSslCertificate(boolean wifiRequired);

    @Override
    HttpDownload scheduleOn(Schedulers schedulers);

    @Override
    HttpDownload setNetworkInterceptors(Interceptor interceptor);

    @Override
    HttpDownload setInterceptors(Call.Interceptor interceptor);

    @Override
    HttpDownload setOnExecuteListener(OnExecuteListener executeListener);
}
