package com.ok.request.listener;

import com.ok.request.base.DownloadExecutor;

public interface OnDownloadListener {
    /**
     * 下载完成
     *
     * @param request
     */
    void onComplete(DownloadExecutor request);

    void onFailure(DownloadExecutor request);

    class IMPL implements OnDownloadListener {

        @Override
        public void onComplete(DownloadExecutor request) {

        }

        @Override
        public void onFailure(DownloadExecutor request) {

        }
    }
}
