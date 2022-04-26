package com.x.down.made;

import java.io.Serializable;

public class DownloaderInfo implements Serializable {
    protected String contentType;
    protected long contentLength;//下载的文件长度

    public DownloaderInfo() {
    }

    public DownloaderInfo(String contentType, long contentLength) {
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
