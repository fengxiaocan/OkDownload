package com.x.down.task;

import java.io.File;

interface MultiDownloadTask {

    /**
     * 获取块已下载的长度
     *
     * @return
     */
    long blockSofar();

    /**
     * 获取块的文件地址
     *
     * @return
     */
    File blockFile();
}
