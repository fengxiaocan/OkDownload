package com.ok.request.request;

import com.ok.request.params.MediaType;

import java.io.Closeable;
import java.io.InputStream;
import java.io.Reader;

public interface ResponseBody extends Closeable {
    String encoding();

    String charset();

    MediaType contentType();

    long contentLength();

    Reader reader();

    InputStream source();
}
