package com.x.down.listener;

import com.x.down.base.IDownloadRequest;

public interface OnDownloadConnectListener {
    /**
     * 正在预备状态,线程挂起或者准备开始
     *
     * @param request
     */
    void onPending(IDownloadRequest request);

    /**
     * 正在开始,线程开始执行
     *
     * @param request
     */
    void onStart(IDownloadRequest request);

    /**
     * 请求连接中
     *
     * @param request
     */
    void onConnecting(IDownloadRequest request);

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

    class IMPL implements OnDownloadConnectListener{

        @Override
        public void onPending(IDownloadRequest request) {

        }

        @Override
        public void onStart(IDownloadRequest request) {

        }

        @Override
        public void onConnecting(IDownloadRequest request) {

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
