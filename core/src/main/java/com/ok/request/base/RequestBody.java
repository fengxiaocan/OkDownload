package com.ok.request.base;

import com.ok.request.params.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public interface RequestBody {

    MediaType contentType();

    long contentLength();

    void writeTo(IoSink io) throws IOException;

    class Body {
        public static RequestBody createFromFile(File file, MediaType mediaType) {
            return new FileBody(file, mediaType);
        }

        public static RequestBody createFromFile(String file, MediaType mediaType) {
            return new FileBody(new File(file), mediaType);
        }

        public static RequestBody create(String value, MediaType mediaType) {
            return new StringBody(value, mediaType);
        }

        public static RequestBody create(String value) {
            return new StringBody(value, MediaType.TEXT);
        }

        static class FileBody implements RequestBody {
            private final File file;
            private final MediaType mediaType;

            protected FileBody(File file, MediaType mediaType) {
                this.file = file;
                this.mediaType = mediaType;
            }

            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public long contentLength() {
                return file.length();
            }

            @Override
            public void writeTo(IoSink buffer) throws IOException {
                FileInputStream inputStream = new FileInputStream(file);
                byte[] bytes = new byte[1024 * 8];
                int length;
                while ((length = inputStream.read(bytes)) > 0) {
                    buffer.writeBytes(bytes, 0, length);
                }
                inputStream.close();
            }
        }

        static class StringBody implements RequestBody {
            private final String content;
            private final MediaType mediaType;

            protected StringBody(String content, MediaType mediaType) {
                this.content = content;
                this.mediaType = mediaType;
            }

            @Override
            public MediaType contentType() {
                return MediaType.parse(mediaType.getType() + "; charset=utf-8");
            }

            @Override
            public long contentLength() {
                return -1L;
            }

            @Override
            public void writeTo(IoSink buffer) throws IOException {
                buffer.writeUtf8(content);
            }
        }
    }

}
