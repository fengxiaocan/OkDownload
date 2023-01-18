package com.ok.request.request;

import com.ok.request.params.Headers;

import java.io.IOException;

public class URLResponse implements Response {

    private final Request request;
    private final ResponseBody responseBody;
    private final long sentRequestAtMillis;
    private final long lastModified;
    private final int code;
    private final Headers headers;

    public URLResponse(Request request, ResponseBody responseBody, int code, Headers headers, long sentRequestAtMillis, long lastModified) {
        this.request = request;
        this.responseBody = responseBody;
        this.sentRequestAtMillis = sentRequestAtMillis;
        this.lastModified = lastModified;
        this.code = code;
        this.headers = headers;
    }

    @Override
    public ResponseBody body() {
        return responseBody;
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public long date() {
        return sentRequestAtMillis;
    }

    @Override
    public long lastModified() {
        return lastModified;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public Headers headers() {
        return headers;
    }

    @Override
    public boolean isSuccess() {
        return code >= 200 && code < 400;
    }

    @Override
    public void close() throws IOException {
        responseBody.close();
    }
}
