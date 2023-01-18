package com.ok.request.request;

import com.ok.request.params.Headers;

import java.io.File;
import java.io.IOException;

public class FileResponse implements Response {
    private final Request request;
    private final ResponseBody responseBody;
    private final long sentRequestAtMillis;
    private final long lastModified;
    private final int code;
    private final Headers headers;

    public FileResponse(Request request, File file) throws Exception {
        this.request = request;
        this.responseBody = new FileResponseBody(file);
        this.sentRequestAtMillis = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
        this.code = 200;
        this.headers = request.headers();
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
        return true;
    }

    @Override
    public void close() throws IOException {
        responseBody.close();
    }
}
