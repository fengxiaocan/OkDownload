package com.x.down.base;

import java.io.IOException;

public interface IoSink {

    IoSink writeByte(int source) throws IOException;

    IoSink writeByte(byte source) throws IOException;

    IoSink writeBytes(byte[] source) throws IOException;

    IoSink writeBytes(byte[] source, int offset, int byteCount) throws IOException;

    IoSink writeUtf8(String string) throws IOException;

    IoSink writeUtf8(String string, int beginIndex, int endIndex) throws IOException;
}
