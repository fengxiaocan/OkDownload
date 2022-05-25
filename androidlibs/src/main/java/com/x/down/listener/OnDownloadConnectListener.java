package com.x.down.listener;

import com.x.down.base.IDownloadRequest;
import com.x.down.data.Headers;

public interface OnDownloadConnectListener {

    /**
     * 请求连接中
     *
     * @param request
     */
    void onConnecting(IDownloadRequest request, Headers headers);

    /**
     * 请求出错了--所有重试已执行
     *
     * @param request
     */
    void onRequestError(IDownloadRequest request, int code, String error);

    /**
     * 请求取消
     *
     * @param request
     */
    void onCancel(IDownloadRequest request);

    /**
     * 请求正在重试
     *
     * @param request
     */
    void onRetry(IDownloadRequest request);

    class IMPL implements OnDownloadConnectListener {

        @Override
        public void onConnecting(IDownloadRequest request, Headers headers) {

        }

        @Override
        public void onRequestError(IDownloadRequest request, int code, String error) {

        }

        @Override
        public void onCancel(IDownloadRequest request) {

        }

        @Override
        public void onRetry(IDownloadRequest request) {

        }
    }
}
