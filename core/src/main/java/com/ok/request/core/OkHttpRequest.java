package com.ok.request.core;

import com.ok.request.base.Call;
import com.ok.request.base.HttpConnect;
import com.ok.request.base.RequestBody;
import com.ok.request.call.Interceptor;
import com.ok.request.config.Config;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.factory.ThreadTaskFactory;
import com.ok.request.listener.OnExecuteListener;
import com.ok.request.listener.OnResponseListener;
import com.ok.request.listener.SSLCertificateFactory;
import com.ok.request.params.Headers;
import com.ok.request.params.Params;
import com.ok.request.request.Request;
import com.ok.request.request.URLRequest;
import com.ok.request.tool.XDownUtils;

import javax.net.ssl.SSLSocketFactory;

public class OkHttpRequest extends BaseRequest implements HttpConnect {
    protected Method method = Method.GET;//请求方法
    protected RequestBody requestBody;//请求体,只有POST方法才有
    protected OnResponseListener onResponseListeners;

    protected OkHttpRequest(String baseUrl) {
        super(baseUrl);
    }

    public static OkHttpRequest with(String url) {
        return new OkHttpRequest(url);
    }

    public OnResponseListener getOnResponseListeners() {
        return onResponseListeners;
    }


    @Override
    public HttpConnect setRequestMethod(Method method) {
        this.method = method;
        return this;
    }

    @Override
    public HttpConnect setOnResponseListener(OnResponseListener listener) {
        onResponseListeners = listener;
        return this;
    }

    @Override
    public HttpConnect post() {
        this.method = Method.POST;
        return this;
    }

    @Override
    public HttpConnect requestBody(RequestBody body) {
        this.requestBody = body;
        return this;
    }

    @Override
    public HttpConnect setTag(Object tag) {
        return (HttpConnect) super.setTag(tag);
    }

    @Override
    public HttpConnect setSSLCertificate(String path) {
        return (HttpConnect) super.setSSLCertificate(path);
    }

    @Override
    public HttpConnect setSSLCertificateFactory(SSLCertificateFactory factory) {
        return (HttpConnect) super.setSSLCertificateFactory(factory);
    }

    @Override
    public HttpConnect addParams(String name, String value) {
        return (HttpConnect) super.addParams(name, value);
    }

    @Override
    public HttpConnect addHeader(String name, String value) {
        return (HttpConnect) super.addHeader(name, value);
    }

    @Override
    public HttpConnect setParams(Params params) {
        return (HttpConnect) super.setParams(params);
    }

    @Override
    public HttpConnect setHeader(Headers header) {
        return (HttpConnect) super.setHeader(header);
    }

    @Override
    public HttpConnect setUserAgent(String userAgent) {
        return (HttpConnect) super.setUserAgent(userAgent);
    }

    @Override
    public HttpConnect setTimeOut(int iOTimeOut) {
        return (HttpConnect) super.setTimeOut(iOTimeOut);
    }

    @Override
    public HttpConnect setUseAutoRetry(boolean useAutoRetry) {
        return (HttpConnect) super.setUseAutoRetry(useAutoRetry);
    }

    @Override
    public HttpConnect setAutoRetryTimes(int autoRetryTimes) {
        return (HttpConnect) super.setAutoRetryTimes(autoRetryTimes);
    }

    @Override
    public HttpConnect setAutoRetryInterval(int autoRetryInterval) {
        return (HttpConnect) super.setAutoRetryInterval(autoRetryInterval);
    }

    @Override
    public HttpConnect setOnExecuteListener(OnExecuteListener executeListener) {
        return (HttpConnect) super.setOnExecuteListener(executeListener);
    }

    @Override
    public HttpConnect permitAllSslCertificate(boolean wifiRequired) {
        return (HttpConnect) super.permitAllSslCertificate(wifiRequired);
    }

    @Override
    public HttpConnect scheduleOn(Schedulers schedulers) {
        return (HttpConnect) super.scheduleOn(schedulers);
    }

    @Override
    public HttpConnect setNetworkInterceptors(Interceptor interceptor) {
        return (HttpConnect) super.setNetworkInterceptors(interceptor);
    }

    @Override
    public HttpConnect setInterceptors(Call.Interceptor interceptor) {
        return (HttpConnect) super.setInterceptors(interceptor);
    }

    @Override
    public Dispatcher start() {
        return ThreadTaskFactory.createHttpRequestTask(this);
    }

    @Override
    public Request request() throws Exception {
        return request(getConnectUrl());
    }

    @Override
    public Request request(String url) throws Exception {
        URLRequest request = new URLRequest(url);
        request.method(method.getMethod());
        request.setHeaders(headers);
        request.body(requestBody);
        request.TimeOut(iOTimeOut);
        //处理https证书
        SSLSocketFactory certificate = null;
        if (sslCertificateFactory != null) {
            certificate = sslCertificateFactory.createCertificate();
        } else if (certificatePath != null) {
            certificate = XDownUtils.getCertificate(certificatePath);
        } else if (permitAllSslCertificate) {
            //允许所有的https证书
            if (Config.ANDROID_SDK_VER29) {
                certificate = XDownUtils.getUnSafeCertificate();
            }
        }
        request.certificate(certificate);
        return request;
    }


}
