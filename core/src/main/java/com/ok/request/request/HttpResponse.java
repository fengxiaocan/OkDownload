package com.ok.request.request;

import com.ok.request.params.Headers;
import com.ok.request.tool.XDownUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HttpResponse {
    private final Request request;
    private final String result;
    private final int code;
    private final String error;
    private final Headers headers;
    private final long date;
    private final long lastModified;

    public HttpResponse(Response response) throws IOException {
        request = response.request();
        code = response.code();
        headers = response.headers();
        date = response.date();
        lastModified = response.lastModified();
        if (response.isSuccess()) {
            result = readStringStream(response.body().source(), response.body().charset());
            error = null;
        } else {
            error = readStringStream(response.body().source(), response.body().charset());
            result = null;
        }
    }

    private String readStringStream(InputStream is, String charset) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, charset));
            StringBuilder builder = new StringBuilder();
            char[] temp = new char[1024 * 8];
            int length;
            while ((length = reader.read(temp)) > 0) {
                builder.append(temp, 0, length);
            }
            return builder.toString();
        } finally {
            XDownUtils.closeIo(is);
            XDownUtils.closeIo(reader);
        }
    }

    public Request request() {
        return request;
    }

    public String result() {
        return result;
    }

    public int code() {
        return code;
    }

    public String error() {
        return error;
    }

    public Headers headers() {
        return headers;
    }

    public long date() {
        return date;
    }

    public long lastModified() {
        return lastModified;
    }

    public boolean isSuccess() {
        return code >= 200 && code < 400;
    }
}
