package com.ok.request.down;

import com.ok.request.base.DownloadExecutor;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.disposer.ProgressDisposer;
import com.ok.request.disposer.SpeedDisposer;
import com.ok.request.listener.OnDownloadListener;
import com.ok.request.tool.XDownUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

final class MultiDownloadDisposer {

    private final OkDownloadRequest request;
    private final int blockCount;
    private final OnDownloadListener onDownloadListener;
    private final ProgressDisposer progressDisposer;
    private final SpeedDisposer speedDisposer;
    private final LinkedList<MultiDownloadBlock> taskList = new LinkedList<>();
    private final LinkedList<File> tempFileList = new LinkedList<>();

    private final CountDownLatch countDownLatch;
    private final AtomicInteger success = new AtomicInteger(0);
    private final AtomicLong sofar = new AtomicLong(0);
    private final AtomicInteger speedLength = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();

    private final long totalLength;

    public MultiDownloadDisposer(OkDownloadRequest request, CountDownLatch countDownLatch,
                                 int blockCount, long totalLength) {
        this.request = request;
        this.onDownloadListener = request.downloadListener();

        final boolean ignoredProgress = request.isIgnoredProgress();
        final int updateProgressTimes = request.getUpdateProgressTimes();
        this.progressDisposer = new ProgressDisposer(ignoredProgress, updateProgressTimes, request);

        final boolean ignoredSpeed = request.isIgnoredSpeed();
        final int updateSpeedTimes = request.getUpdateSpeedTimes();
        this.speedDisposer = new SpeedDisposer(ignoredSpeed, updateSpeedTimes, request);

        this.countDownLatch = countDownLatch;
        this.blockCount = blockCount;
        this.totalLength = totalLength;
    }

    public void addTask(MultiDownloadBlock task) {
        taskList.add(task);
        tempFileList.add(task.blockFile());
    }

    public void removeTask(MultiDownloadBlock task) {
        lock.lock();
        try {
            sofar.getAndAdd(task.blockSofar());
            taskList.remove(task);
        } finally {
            lock.unlock();
        }
    }

    public void onProgress(DownloadExecutor request, int length) {
        if (length>0) {
            speedLength.addAndGet(length);
            if (progressDisposer.isCallProgress()) {
                progressDisposer.onProgress(request, totalLength, getSofarLength());
            }
            if (speedDisposer.isCallSpeed()) {
                speedDisposer.onSpeed(request, speedLength.getAndSet(0));
            }
        }
    }

    public long getSofarLength() {
        lock.lock();
        try {
            long length = sofar.get();
            for (MultiDownloadBlock downloadTask : taskList) {
                length += downloadTask.blockSofar();
            }
            return length;
        } finally {
            lock.unlock();
        }
    }


    public synchronized void onFinish() {
        //计数器
        countDownLatch.countDown();
    }

    public synchronized void onComplete(final DownloadExecutor executor) throws Throwable {
        success.getAndIncrement();

        if (success.get() == blockCount) {
            if (!progressDisposer.isIgnoredProgress()) {
                progressDisposer.onProgress(executor, 1, totalLength, totalLength);
            }
            if (!speedDisposer.isIgnoredSpeed()) {
                speedDisposer.onSpeed(executor, speedLength.get());
            }
            speedLength.set(0);

            File file = request.getSaveFile();
            file.getParentFile().mkdirs();

            byte[] bytes = new byte[1024 * 8];
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file,false);
                for (File tempFile : tempFileList) {
                    FileInputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(tempFile);
                        int length;
                        while ((length = inputStream.read(bytes)) > 0) {
                            outputStream.write(bytes, 0, length);
                            outputStream.flush();
                        }
                        tempFile.deleteOnExit();
                    } finally {
                        XDownUtils.closeIo(inputStream);
                    }
                }
                //完成回调
                if (onDownloadListener != null) {
                    Schedulers schedulers = request.schedulers();
                    if (schedulers != null) {
                        schedulers.schedule(new Runnable() {
                            @Override
                            public void run() {
                                onDownloadListener.onComplete(executor);
                            }
                        });
                    } else {
                        onDownloadListener.onComplete(executor);
                    }
                }
            } finally {
                taskList.clear();
                XDownUtils.closeIo(outputStream);
            }
        } else if (success.get() > blockCount) {
            taskList.clear();
            throw new RuntimeException("The downloaded ts is failure!");
        }
    }
}
