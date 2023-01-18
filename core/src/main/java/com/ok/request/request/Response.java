package com.ok.request.request;

import com.ok.request.params.Headers;

import java.io.Closeable;

public interface Response extends Closeable {
    ResponseBody body();

    Request request();

    long date();

    long lastModified();

    int code();

    Headers headers();

    boolean isSuccess();
}
