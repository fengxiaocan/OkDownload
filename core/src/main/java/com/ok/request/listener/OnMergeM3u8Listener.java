package com.ok.request.listener;

import com.ok.request.base.DownloadExecutor;
import com.ok.request.info.M3U8Info;

import java.io.File;

public interface OnMergeM3u8Listener {
    /**
     * @param request 请求
     * @param m3u8Dir m3u8文件夹
     * @param info    m3u8信息
     * @throws Exception
     */
    void onM3u8Merge(DownloadExecutor request, File m3u8Dir, M3U8Info info) throws Exception;
}
