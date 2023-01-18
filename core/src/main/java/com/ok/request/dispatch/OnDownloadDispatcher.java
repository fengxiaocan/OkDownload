package com.ok.request.dispatch;

import com.ok.request.listener.OnM3u8ParseIntercept;
import com.ok.request.listener.OnMergeM3u8Listener;
import com.ok.request.listener.OnProgressListener;
import com.ok.request.listener.OnSpeedListener;

public interface OnDownloadDispatcher extends OnDispatcher {

    //下载进度监听
    OnProgressListener progressListener();

    //下载速度监听
    OnSpeedListener speedListener();

    //文件合并监听
    OnMergeM3u8Listener mergeM3u8Listener();

    //m3u8解析拦截器
    OnM3u8ParseIntercept m3u8ParseIntercept();
}
