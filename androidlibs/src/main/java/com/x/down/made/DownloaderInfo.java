package com.x.down.made;

import java.io.Serializable;

public class DownloaderInfo implements Serializable {
    protected long contentLength;//下载的文件长度
    protected boolean isAccecp = false;//是否支持断点下载

    public DownloaderInfo() {
    }

    public DownloaderInfo(long contentLength, boolean isAccecp) {
        this.contentLength = contentLength;
        this.isAccecp = isAccecp;
    }

    public boolean isAccecp() {
        return isAccecp;
    }

    public void setAccecp(boolean accecp) {
        isAccecp = accecp;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

}
