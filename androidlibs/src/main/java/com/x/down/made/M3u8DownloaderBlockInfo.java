package com.x.down.made;

import java.io.Serializable;

public class M3u8DownloaderBlockInfo implements Serializable {
    private String name;//名称
    private String url;//下载地址
    private long contentLength;

    public M3u8DownloaderBlockInfo() {
    }

    public M3u8DownloaderBlockInfo(M3u8DownloaderBlock block, long contentLength) {
        this.name = block.getName();
        this.url = block.getUrl();
        this.contentLength = contentLength;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
}
