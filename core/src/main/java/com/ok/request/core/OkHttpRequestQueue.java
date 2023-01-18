package com.ok.request.core;

import com.ok.request.dispatch.Dispatcher;
import com.ok.request.factory.ThreadTaskFactory;

import java.util.ArrayList;
import java.util.List;

public final class OkHttpRequestQueue extends OkHttpRequest {
    protected List<String> queue;

    protected OkHttpRequestQueue(List<String> queue) {
        super("");
        this.queue = queue;
    }

    protected OkHttpRequestQueue() {
        super("");
        this.queue = new ArrayList<>();
    }

    public static OkHttpRequestQueue with(List<String> queue) {
        return new OkHttpRequestQueue(queue);
    }

    public static OkHttpRequestQueue create() {
        return new OkHttpRequestQueue(new ArrayList<String>());
    }

    public OkHttpRequestQueue addRequest(String url) {
        queue.add(url);
        return this;
    }

    public List<OkHttpRequest> cloneToRequest() {
        List<OkHttpRequest> xHttpRequests = new ArrayList<>();
        for (String baseUrl : queue) {
            OkHttpRequest request = new OkHttpRequest(baseUrl);
            request.headers = headers;
            request.params = params;
            request.schedulers = schedulers;
            request.userAgent = userAgent;
            request.permitAllSslCertificate = permitAllSslCertificate;
            request.isUseAutoRetry = isUseAutoRetry;
            request.autoRetryTimes = autoRetryTimes;
            request.autoRetryInterval = autoRetryInterval;
            request.method = method;
            request.requestBody = requestBody;
            request.onResponseListeners = onResponseListeners;
            xHttpRequests.add(request);
        }
        return xHttpRequests;
    }

    @Override
    public Dispatcher start() {
        final List<Dispatcher> list = ThreadTaskFactory.createHttpRequestTaskQueue(this);
        return new Dispatcher() {
            @Override
            public void cancel() {
                for (Dispatcher dispatcher : list) {
                    dispatcher.cancel();
                }
            }

            @Override
            public int state() {
                return 0;
            }

            @Override
            public long id() {
                return 0;
            }

            @Override
            public String name() {
                return "ALL";
            }
        };
    }
}
