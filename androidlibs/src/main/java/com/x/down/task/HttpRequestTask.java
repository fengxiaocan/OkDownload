package com.x.down.task;


import com.x.down.XDownload;
import com.x.down.base.IConnectRequest;
import com.x.down.base.IRequest;
import com.x.down.base.RequestBody;
import com.x.down.core.HttpConnect;
import com.x.down.core.XHttpRequest;
import com.x.down.data.Headers;
import com.x.down.data.MediaType;
import com.x.down.data.Response;
import com.x.down.impl.RequestListenerDisposer;
import com.x.down.made.AutoRetryRecorder;
import com.x.down.tool.XDownUtils;

import java.net.HttpURLConnection;
import java.util.concurrent.Future;

class HttpRequestTask extends BaseHttpRequest implements IRequest, IConnectRequest {

    protected final XHttpRequest httpRequest;
    protected final RequestListenerDisposer listenerDisposer;
    protected Future taskFuture;

    public HttpRequestTask(XHttpRequest request) {
        super(new AutoRetryRecorder(request.isUseAutoRetry(),
                request.getAutoRetryTimes(),
                request.getAutoRetryInterval()));
        this.listenerDisposer = new RequestListenerDisposer(request);
        this.httpRequest = request;
    }

    public final void setTaskFuture(Future taskFuture) {
        this.taskFuture = taskFuture;
    }

    @Override
    public void run() {
        super.run();
        XDownload.get().removeRequest(httpRequest.getTag());
        taskFuture = null;
    }

    @Override
    protected void onExecute() throws Exception {
        HttpURLConnection http = httpRequest.buildConnect();

        //POST请求
        if (httpRequest.isPost()) {
            RequestBody body = httpRequest.getRequestBody();
            if (body != null) {
                MediaType mediaType = body.contentType();
                if (mediaType.getType() != null) {
                    http.setRequestProperty("Content-Type", mediaType.getType());
                }
                if (body.contentLength() != -1) {
                    http.setRequestProperty("Content-Length", String.valueOf(body.contentLength()));
                }
                HttpIoSink ioSink = new HttpIoSink(http.getOutputStream());
                body.writeTo(ioSink);
            }
        }

        listenerDisposer.onConnecting(this, getHeaders(http));
        int responseCode = http.getResponseCode();

        //是否需要重定向
        while (isNeedRedirects(responseCode)) {
            http = redirectsConnect(http, httpRequest);
            if (responseCode == 307) {
                http.setRequestMethod("GET");
            }
            http.connect();
            listenerDisposer.onConnecting(this, getHeaders(http));

            if (http.getRequestMethod().equals("POST")) {
                RequestBody body = httpRequest.getRequestBody();
                if (body != null) {
                    MediaType mediaType = body.contentType();
                    if (mediaType.getType() != null) {
                        http.setRequestProperty("Content-Type", mediaType.getType());
                    }
                    if (body.contentLength() != -1) {
                        http.setRequestProperty("Content-Length", String.valueOf(body.contentLength()));
                    }
                    HttpIoSink ioSink = new HttpIoSink(http.getOutputStream());
                    body.writeTo(ioSink);
                }
            }
            responseCode = http.getResponseCode();
        }
        //请求头
        Headers headers = getHeaders(http);

        if (isSuccess(responseCode)) {
            String stream = readStringStream(http.getInputStream(), XDownUtils.getInputCharset(http));
            XDownUtils.disconnectHttp(http);
            listenerDisposer.onResponse(this, Response.builderSuccess(httpRequest.getConnectUrl(), stream, responseCode, headers));
        } else {
            String error = readStringStream(http.getErrorStream(), XDownUtils.getInputCharset(http));
            XDownUtils.disconnectHttp(http);
            listenerDisposer.onResponse(this, Response.builderFailure(httpRequest.getConnectUrl(), responseCode, headers, error));
            retryToRun();
        }
    }

    @Override
    protected void onRetry() {
        listenerDisposer.onRetry(this);
    }

    @Override
    protected void onError(Throwable e) {
        listenerDisposer.onError(this, e);
    }

    @Override
    protected void onCancel() {
        listenerDisposer.onCancel(this);
    }

    @Override
    public HttpConnect request() {
        return httpRequest;
    }

    @Override
    public String tag() {
        return httpRequest.getTag();
    }

    @Override
    public String url() {
        return httpRequest.getConnectUrl();
    }

    @Override
    public boolean cancel() {
        isCancel = true;
        if (taskFuture != null) {
            return taskFuture.cancel(true);
        }
        return false;
    }

    @Override
    public int retryCount() {
        return autoRetryRecorder.getRetryCount();
    }
}
