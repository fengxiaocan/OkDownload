package com.ok.request.dispatch;

public interface Dispatcher {
    void cancel();

    /**
     * 任务状态
     *
     * @return
     */
    int state();

    long id();

    String name();
}
