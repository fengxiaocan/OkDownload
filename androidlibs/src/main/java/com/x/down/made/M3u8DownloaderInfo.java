package com.x.down.made;

import java.io.Serializable;
import java.util.ArrayList;

public class M3u8DownloaderInfo implements Serializable {
    protected String originalUrl;//原始地址
    protected String redirectUrl;//重定向地址
    protected String originalResponse;//原始地址响应报文
    protected String redirectResponse;//重定向地址响应报文
    protected ArrayList<M3u8DownloaderBlock> blockList;//下载列表地址

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public ArrayList<M3u8DownloaderBlock> getBlockList() {
        return blockList;
    }

    public void setBlockList(ArrayList<M3u8DownloaderBlock> blockList) {
        this.blockList = blockList;
    }

    public String getOriginalResponse() {
        return originalResponse;
    }

    public void setOriginalResponse(String originalResponse) {
        this.originalResponse = originalResponse;
    }

    public String getRedirectResponse() {
        return redirectResponse;
    }

    public void setRedirectResponse(String redirectResponse) {
        this.redirectResponse = redirectResponse;
    }
}
