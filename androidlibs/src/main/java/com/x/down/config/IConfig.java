package com.x.down.config;

public interface IConfig {
    int MULTI_THREAD_MAX_DOWNLOAD_SIZE = 100 * 1024;

    int MULTI_THREAD_MIN_DOWNLOAD_SIZE = 50 * 1024;

    /**
     * 默认缓存路径文件夹
     *
     * @param cacheDir
     * @return
     */
    IConfig cacheDir(String cacheDir);

    /**
     * 默认保存路径文件夹
     *
     * @param dir
     * @return
     */
    IConfig saveDir(String dir);

    /**
     * 默认日记路径文件夹
     *
     * @param dir
     * @return
     */
    IConfig recordDir(String dir);

    /**
     * 同时进行Execute线程请求的最大任务数
     *
     * @param maxCount
     * @return
     */
    IConfig maxExecuteTaskCount(int maxCount);

    /**
     * 允许同时下载的最大任务数量
     *
     * @param maxCount
     * @return
     */
    IConfig downloadMaxTaskCount(int maxCount);

    /**
     * 默认UA
     *
     * @param userAgent
     * @return
     */
    IConfig userAgent(String userAgent);

    /**
     * 写文件buff大小，该数值大小不能小于2048，数值变小，下载速度会变慢
     *
     * @param buffSize
     * @return
     */
    IConfig bufferedSize(int buffSize);

    /**
     * 默认下载的多线程数
     *
     * @param multiThreadCount
     * @return
     */
    IConfig downloadMultiThreadSize(int multiThreadCount);

    /**
     * 默认多线程下载的单线程最大下载文件块大小,默认5MB,最小不能低于100KB
     *
     * @param multiThreadMaxSize
     * @return
     */
    IConfig maxDownloadBlockSize(int multiThreadMaxSize);

    /**
     * 默认多线程下载的单线程最大下载文件块大小,默认100kb,最小不能低于50KB
     *
     * @param multiThreadMinSize
     * @return
     */
    IConfig minDownloadBlockSize(int multiThreadMinSize);

    /**
     * 是否默认开启多线程下载,默认为true
     *
     * @param isUseMultiThread false -- 使用单线程下载文件
     * @return
     */
    IConfig isUseMultiThread(boolean isUseMultiThread);

    /**
     * 是否使用断点续传下载,默认为true
     *
     * @param isUseBreakpointResume true -- 每次检测到未下载完成的都会重新开始下载
     * @return
     */
    IConfig isUseBreakpointResume(boolean isUseBreakpointResume);

    /**
     * 是否忽略下载的progress回调,默认为false
     *
     * @param ignoredProgress true -- 会忽略下载的progress回调,减少性能消耗
     * @return
     */
    IConfig ignoredProgress(boolean ignoredProgress);

    /**
     * 是否忽略下载的速度回调,默认为false
     *
     * @param ignoredSpeed true -- 会忽略下载的速度回调,减少性能消耗
     * @return
     */
    IConfig ignoredSpeed(boolean ignoredSpeed);

    /**
     * 是否使用出错自动重试,默认为true
     *
     * @param isUseAutoRetry false -- 则下载或者请求过程中出错不会自动重试
     * @return
     */
    IConfig isUseAutoRetry(boolean isUseAutoRetry);

    /**
     * 自动重试次数,默认为10次
     *
     * @param autoRetryTimes
     * @return
     */
    IConfig autoRetryTimes(int autoRetryTimes);

    /**
     * 自动重试间隔,毫秒值
     *
     * @param autoRetryInterval 毫秒值
     * @return
     */
    IConfig autoRetryInterval(int autoRetryInterval);

    /**
     * 更新进度条的间隔,毫秒值
     *
     * @param updateProgressTimes 毫秒值
     * @return
     */
    IConfig updateProgressTimes(int updateProgressTimes);

    /**
     * 更新速度的间隔,毫秒值
     *
     * @param updateSpeedTimes 毫秒值
     * @return
     */
    IConfig updateSpeedTimes(int updateSpeedTimes);

    /**
     * 是否允许所有的SSL证书运行,即可以下载所有的https的连接,默认true
     *
     * @param permit false -- 没有证书不能下载https链接
     * @return
     */
    IConfig permitAllSslCertificate(boolean permit);

    /**
     * 连接超时单位为毫秒，默认30秒，该时间不能少于5秒
     *
     * @param connectTimeOut
     * @return
     */
    IConfig connectTimeOut(int connectTimeOut);

    /**
     * 设置IO流读取时间，单位为毫秒，默认20秒，该时间不能少于5秒
     *
     * @param iOTimeOut
     * @return
     */
    IConfig iOTimeOut(int iOTimeOut);

    /**
     * 默认起名名称拦截器,每次下载文件时需要自定义重新命名文件的需要重写
     *
     * @param interceptor
     * @return
     */
    IConfig acquireName(AcquireNameInterceptor interceptor);

}
