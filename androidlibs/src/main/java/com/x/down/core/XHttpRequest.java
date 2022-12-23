package com.x.down.core;

import com.x.down.base.RequestBody;
import com.x.down.data.Headers;
import com.x.down.data.Params;
import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnConnectListener;
import com.x.down.listener.OnRequestInterceptor;
import com.x.down.listener.OnResponseListener;
import com.x.down.listener.SSLCertificateFactory;
import com.x.down.task.ThreadTaskFactory;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class XHttpRequest extends BaseRequest implements HttpConnect, BuilderURLConnection {
    protected Method method = Method.GET;//请求方法
    protected RequestBody requestBody;//请求体,只有POST方法才有
    protected boolean useCaches = false;//是否使用缓存
    protected OnConnectListener onConnectListeners;
    protected OnResponseListener onResponseListeners;
    protected OnRequestInterceptor onRequestInterceptor;

    protected XHttpRequest(String baseUrl) {
        super(baseUrl);
    }

    public static XHttpRequest with(String url) {
        return new XHttpRequest(url);
    }

    public OnConnectListener getOnConnectListeners() {
        return onConnectListeners;
    }

    public OnResponseListener getOnResponseListeners() {
        return onResponseListeners;
    }

    @Override
    public HttpConnect setUseCaches(boolean useCaches) {
        this.useCaches = useCaches;
        return this;
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
    public HttpConnect setOnConnectListener(OnConnectListener listener) {
        onConnectListeners = listener;
        return this;
    }

    @Override
    public HttpConnect setOnRequestInterceptor(OnRequestInterceptor listener) {
        onRequestInterceptor = listener;
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

    public RequestBody getRequestBody() {
        return requestBody;
    }

    public boolean isPost() {
        return method == Method.POST;
    }

    public boolean isUseCaches() {
        return useCaches;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public HttpConnect setTag(String tag) {
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
    public HttpConnect setConnectTimeOut(int connectTimeOut) {
        return (HttpConnect) super.setConnectTimeOut(connectTimeOut);
    }

    @Override
    public HttpConnect setIOTimeOut(int iOTimeOut) {
        return (HttpConnect) super.setIOTimeOut(iOTimeOut);
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
    public HttpConnect permitAllSslCertificate(boolean wifiRequired) {
        return (HttpConnect) super.permitAllSslCertificate(wifiRequired);
    }

    @Override
    public HttpConnect scheduleOn(Schedulers schedulers) {
        return (HttpConnect) super.scheduleOn(schedulers);
    }

    @Override
    public String start() {
        ThreadTaskFactory.createHttpRequestTask(this);
        return getTag();
    }

    @Override
    public HttpURLConnection buildConnect(String connectUrl) throws Exception {
        URL url = new URL(connectUrl);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();

        if (http instanceof HttpsURLConnection) {
            disposeCertificate(http);
        }

        http.setRequestMethod(method.getMethod());
        //设置http请求头
        http.setRequestProperty("Connection", "Keep-Alive");
        if (headers != null) {
            for (String key : headers.keySet()) {
                http.setRequestProperty(key, headers.getValue(key));
            }
        }
        if (userAgent != null) {
            http.setRequestProperty("User-Agent", userAgent);
        }
        http.setConnectTimeout(Math.max(connectTimeOut, 5 * 1000));
        http.setReadTimeout(Math.max(iOTimeOut, 5 * 1000));
        //本次链接是否处理重定向
        http.setInstanceFollowRedirects(false);

        http.setUseCaches(useCaches);
        http.setDoInput(true);
        http.setDoOutput(method == Method.POST);

        if (onRequestInterceptor != null){
            http = onRequestInterceptor.onIntercept(http);
        }
        return http;
    }

    @Override
    public HttpURLConnection buildConnect() throws Exception {
        return buildConnect(getConnectUrl());
    }
}
