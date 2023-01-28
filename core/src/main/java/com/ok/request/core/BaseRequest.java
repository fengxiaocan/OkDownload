package com.ok.request.core;


import com.ok.request.base.Call;
import com.ok.request.base.IConnect;
import com.ok.request.call.Interceptor;
import com.ok.request.config.Config;
import com.ok.request.dispatch.OnDispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.disposer.AutoRetryRecorder;
import com.ok.request.listener.OnExecuteListener;
import com.ok.request.listener.SSLCertificateFactory;
import com.ok.request.params.Headers;
import com.ok.request.params.Params;
import com.ok.request.tool.XDownUtils;

abstract class BaseRequest implements IConnect, OnDispatcher {
    protected final String baseUrl;//下载地址

    protected Object tag;//标记
    protected String certificatePath;//https 证书地址
    protected SSLCertificateFactory sslCertificateFactory;//https 证书创建器
    protected Headers headers;//头部信息
    protected Params params;//参数
    protected Schedulers schedulers;//调度器
    protected OnExecuteListener onExecuteListener;

    protected Interceptor networkInterceptor;
    protected Call.Interceptor interceptor;

    protected String userAgent = Config.config().getUserAgent();//默认UA

    protected boolean permitAllSslCertificate = Config.config().isPermitAllSslCertificate();//是否仅在WiFi情况下下载
    protected boolean isUseAutoRetry = Config.config().isUseAutoRetry();//是否使用出错自动重试
    protected int autoRetryTimes = Config.config().getAutoRetryTimes();//自动重试次数
    protected int autoRetryInterval = Config.config().getAutoRetryInterval();//自动重试间隔
    protected int iOTimeOut = Config.config().getiOTimeOut();//连接超时

    protected volatile String connectUrl;//下载地址
    protected volatile String identifier;//下载标志

    protected BaseRequest(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public final String getConnectUrl() {
        if (connectUrl != null) {
            return connectUrl;
        }
        identifier = null;
        if (params == null || params.size() == 0) {
            return baseUrl;
        }
        StringBuilder builder = new StringBuilder(baseUrl);
        if (baseUrl.indexOf("?") > 0) {
            builder.append("&");
        } else {
            builder.append("?");
        }
        params.toString(builder);
        return connectUrl = builder.toString();
    }

    public String getIdentifier() {
        if (identifier != null) {
            return identifier;
        }
        final String rUrl = getConnectUrl();
        return identifier = XDownUtils.getMd5(rUrl);
    }

    @Override
    public IConnect setTag(Object tag) {
        this.tag = tag;
        return this;
    }

    @Override
    public IConnect setSSLCertificate(String path) {
        this.certificatePath = path;
        return this;
    }

    @Override
    public IConnect setSSLCertificateFactory(SSLCertificateFactory factory) {
        this.sslCertificateFactory = factory;
        return this;
    }

    @Override
    public IConnect addParams(String name, String value) {
        if (params == null) {
            params = new Params();
        }
        params.addParams(name, value);
        this.connectUrl = null;
        this.identifier = null;
        return this;
    }

    @Override
    public IConnect addHeader(String name, String value) {
        if (headers == null) {
            headers = new Headers();
        }
        headers.addHeader(name, value);
        return this;
    }

    @Override
    public IConnect setParams(Params params) {
        this.params = params;
        return this;
    }

    @Override
    public IConnect setHeader(Headers header) {
        this.headers = header;
        return this;
    }

    @Override
    public IConnect setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    @Override
    public IConnect setTimeOut(int iOTimeOut) {
        this.iOTimeOut = iOTimeOut;
        return this;
    }

    @Override
    public IConnect setUseAutoRetry(boolean useAutoRetry) {
        this.isUseAutoRetry = useAutoRetry;
        return this;
    }

    @Override
    public IConnect setAutoRetryTimes(int autoRetryTimes) {
        this.autoRetryTimes = autoRetryTimes;
        return this;
    }

    @Override
    public IConnect setAutoRetryInterval(int autoRetryInterval) {
        this.autoRetryInterval = autoRetryInterval;
        return this;
    }

    @Override
    public IConnect permitAllSslCertificate(boolean permitAllSslCertificate) {
        this.permitAllSslCertificate = permitAllSslCertificate;
        return this;
    }

    @Override
    public IConnect scheduleOn(Schedulers schedulers) {
        this.schedulers = schedulers;
        return this;
    }

    @Override
    public IConnect setOnExecuteListener(OnExecuteListener executeListener) {
        this.onExecuteListener = executeListener;
        return this;
    }

    @Override
    public final Object getTag() {
        if (tag == null) {
            tag = getIdentifier();
        }
        return tag;
    }

    @Override
    public final Schedulers schedulers() {
        return schedulers;
    }

    @Override
    public final AutoRetryRecorder recorder() {
        return new AutoRetryRecorder(isUseAutoRetry, autoRetryTimes, autoRetryInterval);
    }

    @Override
    public final OnExecuteListener executor() {
        return onExecuteListener;
    }

    @Override
    public IConnect setNetworkInterceptors(Interceptor interceptor) {
        this.networkInterceptor = interceptor;
        return this;
    }

    @Override
    public IConnect setInterceptors(Call.Interceptor interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    public Interceptor getNetworkInterceptor() {
        return networkInterceptor;
    }

    public Call.Interceptor getInterceptor() {
        return interceptor;
    }
}
