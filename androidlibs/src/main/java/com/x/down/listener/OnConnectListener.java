package com.x.down.listener;

import com.x.down.base.IRequest;
import com.x.down.data.Headers;

public interface OnConnectListener {
    /**
     * 请求连接中
     *
     * @param request
     */
    void onConnecting(IRequest request, Headers headers);

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
        public void onConnecting(IRequest request, Headers headers) {

        }

        @Override
        public void onCancel(IRequest request) {

        }

        @Override
        public void onRetry(IRequest request) {

        }
    }
}
