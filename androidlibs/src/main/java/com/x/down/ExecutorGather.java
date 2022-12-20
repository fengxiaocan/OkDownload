package com.x.down;


import com.x.down.config.Config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ExecutorGather {
    private static final int MAIN_TASK_KEEP_ALIVE_TIME = 60;//主任务线程保活时间
    private static final int SUBTASK_KEEP_ALIVE_TIME = 1;//子任务线程保活时间

    private static ThreadPoolExecutor downExecutor;
    private static ThreadPoolExecutor taskExecutor;
    private static ThreadPoolExecutor singleExecutor;

    /**
     * 创建子任务线程池队列
     *
     * @return
     */
    public static synchronized ThreadPoolExecutor singleQueue() {
        if (singleExecutor == null) {
            singleExecutor = new ThreadPoolExecutor(1,
                    Integer.MAX_VALUE,
                    SUBTASK_KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());
            singleExecutor.allowCoreThreadTimeOut(true);
        }
        return singleExecutor;
    }


    public static synchronized ThreadPoolExecutor executorTaskQueue() {
        if (taskExecutor == null) {
            int maxCount = Config.config().getMaxExecuteTaskCount();
            taskExecutor = new ThreadPoolExecutor(maxCount,
                    Integer.MAX_VALUE,
                    MAIN_TASK_KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());
            taskExecutor.allowCoreThreadTimeOut(true);
        }
        return taskExecutor;
    }

    /**
     * 创建多线程下载的子任务线程池队列
     */
    public static synchronized ThreadPoolExecutor newSubTaskQueue(int corePoolSize) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize,
                Integer.MAX_VALUE,
                SUBTASK_KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * 创建下载的线程队列
     *
     * @return
     */
    public static synchronized ThreadPoolExecutor executorDownloaderQueue() {
        if (downExecutor == null) {
            int sameTimeDownloadCount = Config.config().getDownloadMaxTaskCount();
            downExecutor = new ThreadPoolExecutor(sameTimeDownloadCount,
                    Integer.MAX_VALUE,
                    MAIN_TASK_KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());
            downExecutor.allowCoreThreadTimeOut(true);
        }
        return downExecutor;
    }

    public static synchronized void recyclerDownloaderQueue() {
        if (downExecutor != null) {
            final ThreadPoolExecutor executor = downExecutor;
            executor.shutdown();
        }
        downExecutor = null;
    }

    public static synchronized void recyclerHttpQueue() {
        if (taskExecutor != null) {
            final ThreadPoolExecutor executor = taskExecutor;
            executor.shutdown();
        }
        taskExecutor = null;
    }

    public static synchronized void recyclerSingleQueue() {
        if (singleExecutor != null) {
            final ThreadPoolExecutor executor = singleExecutor;
            executor.shutdown();
        }
        singleExecutor = null;
    }

    public static synchronized void recyclerAllQueue() {
        recyclerDownloaderQueue();
        recyclerHttpQueue();
        recyclerSingleQueue();
    }

}
