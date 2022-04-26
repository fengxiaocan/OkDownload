package com.x.down.base;

import com.x.down.core.XDownloadRequest;

public interface IDownloadRequest {
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
     * 请求的
     *
     * @return
     */
    XDownloadRequest request();

    /**
     * 获取下载文件地址
     *
     * @return
     */
    String getFilePath();

    /**
     * 获取文件总长度
     *
     * @return
     */
    long getTotalLength();

    /**
     * 获取文件已下载长度
     *
     * @return
     */
    long getSofarLength();
}
