package com.x.down.core;

import com.x.down.base.RequestBody;
import com.x.down.data.Headers;
import com.x.down.data.Params;
import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnConnectListener;
import com.x.down.listener.OnResponseListener;
import com.x.down.listener.SSLCertificateFactory;

public interface HttpConnect extends IConnect {

    @Override
    HttpConnect setTag(String tag);

    @Override
    HttpConnect setSSLCertificate(String path);

    @Override
    HttpConnect setSSLCertificateFactory(SSLCertificateFactory factory);

    @Override
    HttpConnect addParams(String name, String value);

    @Override
    HttpConnect addHeader(String name, String value);

    @Override
    HttpConnect setParams(Params params);

    @Override
    HttpConnect setHeader(Headers header);

    @Override
    HttpConnect setUserAgent(String userAgent);

    @Override
    HttpConnect setConnectTimeOut(int connectTimeOut);

    @Override
    HttpConnect setIOTimeOut(int iOTimeOut);

    @Override
    HttpConnect setUseAutoRetry(boolean useAutoRetry);

    @Override
    HttpConnect setAutoRetryTimes(int autoRetryTimes);

    @Override
    HttpConnect setAutoRetryInterval(int autoRetryInterval);

    @Override
    HttpConnect permitAllSslCertificate(boolean wifiRequired);

    @Override
    HttpConnect scheduleOn(Schedulers schedulers);

    /**
     * 是否使用缓存,只对GET方式有效
     *
     * @param useCaches
     * @return
     */
    HttpConnect setUseCaches(boolean useCaches);

    /**
     * 设置请求的方式
     *
     * @param method
     * @return
     */
    HttpConnect setRequestMethod(Method method);

    /**
     * 设置请求回调监听
     *
     * @param listener
     * @return
     */
    HttpConnect setOnResponseListener(OnResponseListener listener);

    /**
     * 设置连接状态监听
     *
     * @param listener
     * @return
     */
    HttpConnect setOnConnectListener(OnConnectListener listener);

    /**
     * 修改为POST请求
     *
     * @return
     */
    HttpConnect post();

    /**
     * POST请求体
     *
     * @param body
     * @return
     */
    HttpConnect requestBody(RequestBody body);

    String start();

    enum Method {
        GET("GET"),
        POST("POST"),
        PUT("PUT"),
        HEAD("HEAD"),
        DELETE("DELETE");

        private final String method;

        Method(String method) {
            this.method = method;
        }

        public String getMethod() {
            return method;
        }
    }
}
