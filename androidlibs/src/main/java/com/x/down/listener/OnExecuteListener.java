package com.x.down.listener;

import com.x.down.base.IExecute;

public interface OnExecuteListener {
    /**
     * 正在预备状态,线程挂起或者准备开始
     *
     * @param request
     */
    void onPending(IExecute request);

    /**
     * 正在开始,线程开始执行
     *
     * @param request
     */
    void onStart(IExecute request);

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

    class IMPL implements OnExecuteListener{

        @Override
        public void onPending(IExecute request) {

        }

        @Override
        public void onStart(IExecute request) {

        }

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
