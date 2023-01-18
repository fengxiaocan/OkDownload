package com.ok.request;


import com.ok.request.config.Config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class ExecutorGather {
    private static ThreadPoolExecutor downExecutor;
    private static ThreadPoolExecutor taskExecutor;
    private static ThreadPoolExecutor singleExecutor;

    private static long mainTaskKeepAliveTime = 60000;//主任务线程保活时间
    private static long subtaskKeepAliveTime = 1000;//子任务线程保活时间

    public static void setMainTaskKeepAliveTime(long mainTaskKeepAliveTime) {
        ExecutorGather.mainTaskKeepAliveTime = mainTaskKeepAliveTime;
    }

    public static void setSubtaskKeepAliveTime(long subtaskKeepAliveTime) {
        ExecutorGather.subtaskKeepAliveTime = subtaskKeepAliveTime;
    }

    public static void setMainTaskKeepAliveTime(long mainTaskKeepAliveTime, TimeUnit timeUnit) {
        ExecutorGather.mainTaskKeepAliveTime =  timeUnit.toMillis(mainTaskKeepAliveTime);
    }

    public static void setSubtaskKeepAliveTime(long subtaskKeepAliveTime, TimeUnit timeUnit) {
        ExecutorGather.subtaskKeepAliveTime =  timeUnit.toMillis(subtaskKeepAliveTime);
    }

    /**
     * 创建子任务线程池队列
     *
     * @return
     */
    public static synchronized ThreadPoolExecutor singleQueue() {
        if (singleExecutor == null) {
            singleExecutor = new ThreadPoolExecutor(1,
                    Integer.MAX_VALUE,
                    subtaskKeepAliveTime,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(),new DefaultThreadFactory("-Single-"));
            singleExecutor.allowCoreThreadTimeOut(true);
        }
        return singleExecutor;
    }


    public static synchronized ThreadPoolExecutor executorTaskQueue() {
        if (taskExecutor == null) {
            int maxCount = Config.config().getMaxExecuteTaskCount();
            taskExecutor = new ThreadPoolExecutor(maxCount,
                    Integer.MAX_VALUE,
                    mainTaskKeepAliveTime,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(), new DefaultThreadFactory("-Request-"));
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
                subtaskKeepAliveTime,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new DefaultThreadFactory("-SubTask-"));
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
                    mainTaskKeepAliveTime,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(), new DefaultThreadFactory("-Download-"));
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


    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String groupName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "OkPool-" + poolNumber.getAndIncrement() + groupName;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
