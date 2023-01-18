package com.ok.request.down;

import java.io.File;

public interface MultiDownloadBlock {
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
