package com.x.down.made;

import java.io.Serializable;

public final class DownloaderBlock implements Serializable {
    private long contentLength;
    private long blockLength;
    private int threadCount = 1;

    public DownloaderBlock() {
    }

    public DownloaderBlock(long contentLength, long blockLength, int threadCount) {
        this.contentLength = contentLength;
        this.blockLength = blockLength;
        this.threadCount = threadCount;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public long getBlockLength() {
        return blockLength;
    }

    public void setBlockLength(long blockLength) {
        this.blockLength = blockLength;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

}
