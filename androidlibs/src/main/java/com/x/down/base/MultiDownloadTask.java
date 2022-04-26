package com.x.down.base;

import java.io.File;

public interface MultiDownloadTask extends IDownloadRequest {
    /**
     * 下载的线程任务位置
     *
     * @return
     */
    int blockIndex();

    /**
     * 获取下载块的起点
     *
     * @return
     */
    long blockStart();

    /**
     * 获取下载块的终点
     *
     * @return
     */
    long blockEnd();

    /**
     * 获取块已下载的长度
     *
     * @return
     */
    long blockSofarLength();

    /**
     * 获取块的文件地址
     *
     * @return
     */
    File blockFile();
}
