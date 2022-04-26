package com.x.down.listener;

/**
 * 所有的ExecuteQueues队列任务栈全部按照顺序执行完成
 */
public interface OnQueuesCompleteListener {
    /**
     * 执行完成
     */
    void onComplete();
}
