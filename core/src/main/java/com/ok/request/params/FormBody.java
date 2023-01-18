package com.ok.request.params;

import com.ok.request.base.IoSink;
import com.ok.request.base.RequestBody;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class FormBody implements RequestBody {
    private final List<String> encodedNames = new LinkedList<>();
    private final List<String> encodedValues = new LinkedList<>();

    public FormBody addFormData(String name, String value) {
        encodedNames.add(name);
        encodedValues.add(value);
        return this;
    }

    @Override
    public MediaType contentType() {
        return MediaType.CONTENT_TYPE;
    }

    @Override
    public long contentLength() {
        return -1L;
    }

    @Override
    public void writeTo(IoSink buffer) throws IOException {
        for (int i = 0; i < encodedNames.size(); i++) {
            if (i > 0) {
                buffer.writeByte('&');
            }
            buffer.writeUtf8(encodedNames.get(i));
            buffer.writeByte('=');
            buffer.writeUtf8(encodedValues.get(i));
        }
    }

}
