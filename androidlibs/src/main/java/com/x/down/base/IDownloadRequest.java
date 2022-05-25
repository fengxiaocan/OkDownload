package com.x.down.base;

import com.x.down.core.XDownloadRequest;

public interface IDownloadRequest {

    /**
     * 请求的
     *
     * @return
     */
    XDownloadRequest request();

    /**
     * 获取tag
     *
     * @return
     */
    String tag();

    /**
     * 获取下载的Url
     *
     * @return
     */
    String url();

    /**
     * 重试次数
     *
     * @return
     */
    int retryCount();

    /**
     * 获取下载文件地址
     *
     * @return
     */
    String getFilePath();
}
