package com.ok.request.listener;

import com.ok.request.base.Executor;

public interface OnExecuteListener {
    /**
     * 请求被取消了
     *
     * @param executor
     */
    void onStart(Executor executor);

    /**
     * 请求正在重试
     *
     * @param executor
     */
    void onRetry(Executor executor, int retryCoun);

    void onCancel(Executor executor);

    void onError(Executor executor,Throwable e);

    void onComplete(Executor executor);

    class IMPL implements OnExecuteListener {
        @Override
        public void onStart(Executor executor) {

        }

        @Override
        public void onCancel(Executor executor) {

        }

        @Override
        public void onRetry(Executor executor, int retryCoun) {

        }

        @Override
        public void onError(Executor executor, Throwable e) {

        }

        @Override
        public void onComplete(Executor executor) {

        }
    }
}
