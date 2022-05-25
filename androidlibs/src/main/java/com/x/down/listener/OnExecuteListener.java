package com.x.down.listener;

import com.x.down.base.IExecute;

public interface OnExecuteListener {

    /**
     * 请求被取消了
     *
     * @param request
     */
    void onCancel(IExecute request);

    /**
     * 请求正在重试
     *
     * @param request
     */
    void onRetry(IExecute request);

    /**
     * 请求错误
     *
     * @param request
     */
    void onError(IExecute request, Throwable exception);

    class IMPL implements OnExecuteListener {

        @Override
        public void onCancel(IExecute request) {

        }

        @Override
        public void onRetry(IExecute request) {

        }

        @Override
        public void onError(IExecute request, Throwable exception) {

        }
    }
}
