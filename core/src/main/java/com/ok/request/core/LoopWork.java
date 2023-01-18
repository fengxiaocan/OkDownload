package com.ok.request.core;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public final class LoopWork {
    private static LoopWork sInstance;
    private final UUID LOCK = UUID.randomUUID();
    private Thread thread;
    private final Queue<Work> task = new LinkedList();

    private LoopWork() {
    }

    public static synchronized LoopWork get() {
        if (sInstance == null) {
            sInstance = new LoopWork();
        }
        return sInstance;
    }

    public synchronized boolean start() {
        if (thread != null) return thread.isInterrupted();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    Work work;
                    while ((work = task.poll()) != null && !Thread.currentThread().isInterrupted()) {
                        try {
                            work.doWork();
                        } catch (Throwable throwable) {
                            work.tryCatch(throwable);
                        }
                    }
                    if (!Thread.currentThread().isInterrupted()) {
                        waitBlock();
                    }
                }
                if (Thread.currentThread().isInterrupted()) {
                    for (Work work : task) {
                        work.onCancel();
                    }
                }
            }
        }, "LoopWork");
        thread.start();
        return true;
    }

    private void waitBlock() {
        synchronized (LOCK) {
            try {
                LOCK.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void notifyBlock() {
        synchronized (LOCK) {
            try {
                LOCK.notify();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean addWork(Work work) {
        if (thread != null) {
            task.offer(work);
            notifyBlock();
            return true;
        }
        return false;
    }

    public synchronized Thread.State getThreadState() {
        if (thread != null)
            return thread.getState();
        return Thread.State.TERMINATED;
    }

    public synchronized boolean stop() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
            return true;
        }
        return false;
    }

    public interface Work {
        void doWork() throws Throwable;

        void tryCatch(Throwable throwable);

        void onCancel();
    }

    public static abstract class Worker implements Work {

        public boolean isInterrupted() {
            return Thread.currentThread().isInterrupted();
        }

        @Override
        public void tryCatch(Throwable throwable) {
        }

        @Override
        public void onCancel() {
        }
    }
}
