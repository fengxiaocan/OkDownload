package com.x.down.listener;

public interface OnExecuteQueueListener {
    /**
     * 执行完成
     *
     * @param taskCount     任务总数
     * @param completeCount 完成数
     */
    void onComplete(int taskCount, int completeCount);

}
