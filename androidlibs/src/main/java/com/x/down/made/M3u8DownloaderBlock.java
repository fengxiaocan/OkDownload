package com.x.down.made;

import java.io.Serializable;

public class M3u8DownloaderBlock implements Serializable {
    private String name;//名称
    private String url;//下载地址

    public M3u8DownloaderBlock() {
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
}
