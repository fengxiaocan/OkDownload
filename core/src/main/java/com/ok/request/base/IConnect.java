package com.ok.request.base;

import com.ok.request.call.Interceptor;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.listener.OnExecuteListener;
import com.ok.request.listener.SSLCertificateFactory;
import com.ok.request.params.Headers;
import com.ok.request.params.Params;
import com.ok.request.request.Request;

public interface IConnect extends IExecuteRequest{
    Request request() throws Exception;

    Request request(String url) throws Exception;

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
     * 设置IO超时时间
     *
     * @param iOTimeOut
     * @return
     */
    IConnect setTimeOut(int iOTimeOut);

    /**
     * 没有证书的情况下是否允许所有的Https下载链接
     *
     * @param permit
     * @return
     */
    IConnect permitAllSslCertificate(boolean permit);

    IConnect setNetworkInterceptors(Interceptor interceptor);

    IConnect setInterceptors(Call.Interceptor interceptor);

    /**
     * 设置线程标记,通过该标记能够取消当前请求线程
     *
     * @param tag
     * @return
     */
    @Override
    IConnect setTag(Object tag);

    @Override
    Object getTag();

    /**
     * 设置线程调度回调
     *
     * @param schedulers
     * @return
     */
    @Override
    IConnect scheduleOn(Schedulers schedulers);

    /**
     * 设置是否出错自动重试
     *
     * @param useAutoRetry
     * @return
     */
    @Override
    IConnect setUseAutoRetry(boolean useAutoRetry);

    /**
     * 设置出错自动重试的次数
     *
     * @param autoRetryTimes
     * @return
     */
    @Override
    IConnect setAutoRetryTimes(int autoRetryTimes);

    /**
     * 设置自动重试的间隔,毫秒值
     *
     * @param autoRetryInterval
     * @return
     */
    @Override
    IConnect setAutoRetryInterval(int autoRetryInterval);

    @Override
    IConnect setExecuteListener(OnExecuteListener executeListener);
}
