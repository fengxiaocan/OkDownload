package com.x.down.listener;

import com.x.down.base.IRequest;

public interface OnConnectListener {
    /**
     * 正在预备状态,线程挂起或者准备开始
     *
     * @param request
     */
    void onPending(IRequest request);

    /**
     * 正在开始,线程开始执行
     *
     * @param request
     */
    void onStart(IRequest request);

    /**
     * 请求连接中
     *
     * @param request
     */
    void onConnecting(IRequest request);

    /**
     * 请求被取消了
     *
     * @param request
     */
    void onCancel(IRequest request);

    /**
     * 请求正在重试
     *
     * @param request
     */
    void onRetry(IRequest request);

    class IMPL implements OnConnectListener {

        @Override
        public void onPending(IRequest request) {

        }

        @Override
        public void onStart(IRequest request) {

        }

        @Override
        public void onConnecting(IRequest request) {

        }

        @Override
        public void onCancel(IRequest request) {

        }

        @Override
        public void onRetry(IRequest request) {

        }
    }
}
