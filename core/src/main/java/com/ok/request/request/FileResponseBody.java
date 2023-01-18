package com.ok.request.request;

import com.ok.request.params.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class FileResponseBody implements ResponseBody {
    private final String encoding;
    private final String charset;
    private final MediaType mediaType;
    private final InputStream inputStream;
    private final long contentLength;

    public FileResponseBody(File file) throws Exception {
        this.inputStream = new FileInputStream(file);
        this.contentLength = file.length();
        this.encoding = null;
        this.charset = null;
        this.mediaType = null;
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
