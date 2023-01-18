package com.ok.request.executor;


import com.ok.request.CoreDownload;
import com.ok.request.call.RequestCall;
import com.ok.request.core.OkHttpRequest;
import com.ok.request.exception.HttpErrorException;
import com.ok.request.listener.OnResponseListener;
import com.ok.request.request.HttpResponse;
import com.ok.request.request.Response;

public class HttpExecutor extends AutoRetryExecutor {
    protected final OkHttpRequest httpRequest;
    protected final RequestCall httpCall = new RequestCall();

    public HttpExecutor(OkHttpRequest request) {
        super(request);
        CoreDownload.addExecute(tag(), this);
        this.httpRequest = request;
        httpCall.setNetworkInterceptors(httpRequest.getNetworkInterceptor());
        httpCall.setInterceptors(httpRequest.getInterceptor());
    }

    @Override
    protected void onExecute() throws Throwable {
        try {
            Response response = httpCall.process(httpRequest.request());
            final HttpResponse httpResponse = new HttpResponse(response);

            onResult(httpResponse);

            if (!response.isSuccess()) {
                String url = response.request().url().toString();
                throw new HttpErrorException(response.code(), url, httpResponse.error());
            }
        } finally {
            httpCall.terminated();
        }
    }

    private void onResult(final HttpResponse httpResponse) {
        final OnResponseListener listener = httpRequest.getOnResponseListeners();
        if (listener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        listener.onResponse(HttpExecutor.this, httpResponse);
                    }
                });
            } else {
                listener.onResponse(this, httpResponse);
            }
        }
    }

    @Override
    protected void completeRun() {
        CoreDownload.removeExecute(tag(), this);
    }

    @Override
    protected void applyCancel() {
        httpCall.cancel();
    }
}
