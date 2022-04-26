package com.x.down.core;

import com.x.down.data.Headers;
import com.x.down.data.Params;
import com.x.down.dispatch.Schedulers;
import com.x.down.listener.SSLCertificateFactory;

interface IConnect {
    /**
     * 设置线程标记,通过该标记能够取消当前请求线程
     *
     * @param tag
     * @return
     */
    IConnect setTag(String tag);

    /**
     * 设置https请求证书路径
     *
     * @param path
     * @return
     */
    IConnect setSSLCertificate(String path);

    /**
     * 设置https请求证书加载器
     *
     * @param factory
     * @return
     */
    IConnect setSSLCertificateFactory(SSLCertificateFactory factory);

    /**
     * 添加请求参数
     *
     * @param name
     * @param value
     * @return
     */
    IConnect addParams(String name, String value);

    /**
     * 添加请求Header
     *
     * @param name
     * @param value
     * @return
     */
    IConnect addHeader(String name, String value);

    /**
     * 设置请求参数
     *
     * @param params
     * @return
     */
    IConnect setParams(Params params);

    /**
     * 设置请求Header
     *
     * @param header
     * @return
     */
    IConnect setHeader(Headers header);

    /**
     * 设置UA
     *
     * @param userAgent
     * @return
     */
    IConnect setUserAgent(String userAgent);

    /**
     * 设置链接超时时间
     *
     * @param connectTimeOut
     * @return
     */
    IConnect setConnectTimeOut(int connectTimeOut);

    /**
     * 设置IO超时时间
     *
     * @param iOTimeOut
     * @return
     */
    IConnect setIOTimeOut(int iOTimeOut);

    /**
     * 设置是否出错自动重试
     *
     * @param useAutoRetry
     * @return
     */
    IConnect setUseAutoRetry(boolean useAutoRetry);

    /**
     * 设置出错自动重试的次数
     *
     * @param autoRetryTimes
     * @return
     */
    IConnect setAutoRetryTimes(int autoRetryTimes);

    /**
     * 设置自动重试的间隔,毫秒值
     *
     * @param autoRetryInterval
     * @return
     */
    IConnect setAutoRetryInterval(int autoRetryInterval);

    /**
     * 没有证书的情况下是否允许所有的Https下载链接
     *
     * @param permit
     * @return
     */
    IConnect permitAllSslCertificate(boolean permit);

    /**
     * 设置线程调度回调
     *
     * @param schedulers
     * @return
     */
    IConnect scheduleOn(Schedulers schedulers);

    /**
     * 开始任务
     *
     * @return
     */
    String start();
}
