package com.x.down.core;

import com.x.down.listener.OnExecuteQueueListener;

import java.util.List;

public interface IExecuteQueue extends IExecuteRequest {
    /**
     * 添加一个任务队列
     *
     * @param runnable
     * @return
     */
    IExecuteQueue addRequest(Execute runnable);


    /**
     * 设置任务运行的最大线程数
     *
     * @param maxExecuteTaskCount 最大线程数
     * @return
     */
    IExecuteQueue setMaxExecuteTaskCount(int maxExecuteTaskCount);

    /**
     * 设置队列任务完成进度，当队列所有任务都执行（包含失败或者取消的任务）的时候，才会回调
     *
     * @param executeQueueListener
     * @return
     */
    IExecuteQueue setExecuteQueueListener(OnExecuteQueueListener executeQueueListener);

    /**
     * 可以在执行完成现有任务以后再执行提交的下一个队列任务
     * 按顺序执行 队列任务
     *
     * @param thenQueue 下一个队列任务
     * @return 返回当前的任务队列管理器
     */
    IExecuteQueue then(IExecuteQueue thenQueue);

    /**
     * 可以在执行完成现有任务以后再执行提交的下一个队列任务
     * 按顺序执行 队列任务
     *
     * @param runnable 下一个队列任务
     * @return 返回当前的任务队列管理器
     */
    IExecuteQueue then(Execute runnable);

    /**
     * 可以在执行完成现有任务以后再执行提交的下一个队列任务
     * 按顺序执行 队列任务
     *
     * @param runnables 下一个队列任务
     * @return 返回当前的任务队列管理器
     */
    IExecuteQueue then(List<Execute> runnables);
}
