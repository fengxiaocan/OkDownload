package com.ok.request.listener;

import com.ok.request.base.DownloadExecutor;

public interface OnDownloadListener {
    /**
     * 下载完成
     *
     * @param request
     */
    void onComplete(DownloadExecutor request);
}
