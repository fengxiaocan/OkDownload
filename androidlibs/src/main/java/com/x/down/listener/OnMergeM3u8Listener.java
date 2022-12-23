package com.x.down.listener;

import com.x.down.core.XDownloadRequest;
import com.x.down.m3u8.M3U8Info;

import java.io.File;
import java.util.List;

public interface OnMergeM3u8Listener {
    /**
     *
     * @param request 请求
     * @param m3u8Dir m3u8文件夹
     * @param info m3u8信息
     * @throws Exception
     */
    void onM3u8Merge(XDownloadRequest request, File m3u8Dir, M3U8Info info) throws Exception;
}
