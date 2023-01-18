package com.ok.request.request;

import com.ok.request.params.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class HttpResponseBody implements ResponseBody {
    private final String encoding;
    private final String charset;
    private final MediaType mediaType;
    private final InputStream inputStream;
    private final long contentLength;

    public HttpResponseBody(InputStream inputStream, long contentLength, String encoding, String charset, MediaType mediaType) {
        this.inputStream = inputStream;
        this.contentLength = contentLength;
        this.encoding = encoding;
        this.charset = charset;
        this.mediaType = mediaType;
    }

    @Override
    public String encoding() {
        return encoding;
    }

    @Override
    public String charset() {
        return charset;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public Reader reader() {
        return new InputStreamReader(inputStream);
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public InputStream source() {
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
