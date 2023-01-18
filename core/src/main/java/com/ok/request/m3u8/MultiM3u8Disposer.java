package com.ok.request.m3u8;

import com.ok.request.base.DownloadExecutor;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.disposer.ProgressDisposer;
import com.ok.request.disposer.SpeedDisposer;
import com.ok.request.down.MultiDownloadBlock;
import com.ok.request.info.M3U8Info;
import com.ok.request.tool.M3U8Utils;

import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;


class MultiM3u8Disposer {

    private final OkDownloadRequest request;
    private final ProgressDisposer progressDisposer;
    private final SpeedDisposer speedDisposer;

    private final int blockCount;
    private final M3U8Info m3U8Info;
    private final File saveFile;
    private final LinkedList<MultiDownloadBlock> taskList = new LinkedList<>();

    private final CountDownLatch countDownLatch;
    private final AtomicInteger success = new AtomicInteger(0);//成功位置
    private final AtomicLong sofar = new AtomicLong(0);
    private final AtomicInteger speedLength = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();

    public MultiM3u8Disposer(OkDownloadRequest request,
                             CountDownLatch countDownLatch,
                             File saveFile,
                             M3U8Info m3U8Info) {
        this.request = request;
        this.saveFile = saveFile;
        this.blockCount = m3U8Info.getTsList().size();
        this.countDownLatch = countDownLatch;
        this.m3U8Info = m3U8Info;

        final boolean ignoredProgress = request.isIgnoredProgress();
        final int updateProgressTimes = request.getUpdateProgressTimes();
        this.progressDisposer = new ProgressDisposer(ignoredProgress, updateProgressTimes, request);

        final boolean ignoredSpeed = request.isIgnoredSpeed();
        final int updateSpeedTimes = request.getUpdateSpeedTimes();
        this.speedDisposer = new SpeedDisposer(ignoredSpeed, updateSpeedTimes, request);
    }

    public void addTask(MultiDownloadBlock task) {
        taskList.add(task);
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
        if (length > 0) {
            speedLength.addAndGet(length);

            if (progressDisposer.isCallProgress()) {
                float v = success.get() * 1F / blockCount;
                progressDisposer.onProgress(request, v, 0, getSofarLength());
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

    public void onFinish() {
        //计数器
        countDownLatch.countDown();
    }

    public synchronized void onFailure(final DownloadExecutor executor) {
        if (success.get() < blockCount) {
            request.callDownloadFailure(executor);
        }
    }

    public void onComplete(final DownloadExecutor executor) {
        success.getAndIncrement();

        if (success.get() == blockCount) {
            if (!progressDisposer.isIgnoredProgress()) {
                long sofarLength = getSofarLength();
                progressDisposer.onProgress(executor, 1, sofarLength, sofarLength);
            }
            if (!speedDisposer.isIgnoredSpeed()) {
                speedDisposer.onSpeed(executor, speedLength.get());
            }
            speedLength.set(0);
            try {
                if (M3U8Utils.mergeM3u8(executor, request, saveFile, m3U8Info)) {
                    request.callDownloadComplete(executor);
                }
            } finally {
                taskList.clear();
            }
        } else if (success.get() > blockCount) {
            taskList.clear();
            throw new RuntimeException("The Downloaded is failure!");
        }
    }

}
