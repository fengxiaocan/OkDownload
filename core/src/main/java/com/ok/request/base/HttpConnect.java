package com.ok.request.base;

import com.ok.request.call.Interceptor;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.listener.OnExecuteListener;
import com.ok.request.listener.OnResponseListener;
import com.ok.request.listener.SSLCertificateFactory;
import com.ok.request.params.Headers;
import com.ok.request.params.Params;

public interface HttpConnect extends IConnect {
    @Override
    HttpConnect setTag(Object tag);

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
    HttpConnect setTimeOut(int iOTimeOut);

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

    @Override
    HttpConnect setNetworkInterceptors(Interceptor interceptor);

    @Override
    HttpConnect setInterceptors(Call.Interceptor interceptor);

    @Override
    HttpConnect setExecuteListener(OnExecuteListener executeListener);

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
