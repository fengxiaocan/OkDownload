package com.x.down.made;

public class M3u8BlockInfo extends M3u8DownloaderBlock {
    private long contentLength;

    public M3u8BlockInfo() {
    }

    public M3u8BlockInfo(M3u8DownloaderBlock block) {
        setName(block.getName());
        setUrl(block.getUrl());
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

}
