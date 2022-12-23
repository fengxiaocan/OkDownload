package com.x.down.task;

import com.x.down.XDownload;
import com.x.down.base.IDownloadRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.data.Headers;
import com.x.down.impl.DownloadListenerDisposer;
import com.x.down.impl.ProgressDisposer;
import com.x.down.impl.SpeedDisposer;
import com.x.down.listener.OnDownloadConnectListener;
import com.x.down.m3u8.M3U8Info;
import com.x.down.m3u8.M3U8Utils;
import com.x.down.proxy.SerializeProxy;

import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;


class MultiM3u8Disposer implements OnDownloadConnectListener {

    private final XDownloadRequest request;
    private final File saveFile;
    private final int blockCount;
    private final DownloadListenerDisposer listenerDisposer;
    private final ProgressDisposer progressDisposer;
    private final SpeedDisposer speedDisposer;
    private final M3U8Info m3U8Info;
    private final CountDownLatch countDownLatch;
    private final LinkedList<MultiDownloadTask> taskList = new LinkedList<>();
    private final AtomicInteger success = new AtomicInteger(0);//成功位置
    private final AtomicLong sofar = new AtomicLong(0);
    private final AtomicInteger speedLength = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();

    public MultiM3u8Disposer(XDownloadRequest request,
                             CountDownLatch countDownLatch,
                             File saveFile,
                             M3U8Info m3U8Info,
                             DownloadListenerDisposer disposer) {
        this.request = request;
        this.saveFile = saveFile;
        this.blockCount = m3U8Info.getTsList().size();
        this.listenerDisposer = disposer;
        this.countDownLatch = countDownLatch;
        this.m3U8Info = m3U8Info;
        final boolean ignoredProgress = request.isIgnoredProgress();
        final int updateProgressTimes = request.getUpdateProgressTimes();
        this.progressDisposer = new ProgressDisposer(ignoredProgress, updateProgressTimes, disposer);
        final boolean ignoredSpeed = request.isIgnoredSpeed();
        final int updateSpeedTimes = request.getUpdateSpeedTimes();
        this.speedDisposer = new SpeedDisposer(ignoredSpeed, updateSpeedTimes, disposer);
    }

    public void addTask(MultiDownloadTask task) {
        taskList.add(task);
    }

    public void removeTask(MultiDownloadTask task) {
        lock.lock();
        try {
            sofar.getAndAdd(task.blockSofar());
            taskList.remove(task);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onConnecting(IDownloadRequest request, final Headers headers) {
        listenerDisposer.onConnecting(request, headers);
    }

    @Override
    public void onRequestError(IDownloadRequest request, int code, String error) {
        listenerDisposer.onRequestError(request, code, error);
    }

    public void onProgress(IDownloadRequest request, int length) {
        speedLength.addAndGet(length);

        if (progressDisposer.isCallProgress()) {
            float v = success.get() * 1F / blockCount;
            progressDisposer.onProgress(request, v, 0, getSofarLength());
        }
        if (speedDisposer.isCallSpeed()) {
            speedDisposer.onSpeed(request, speedLength.getAndSet(0));
        }
    }

    public long getSofarLength() {
        lock.lock();
        try {
            long length = sofar.get();
            for (MultiDownloadTask downloadTask : taskList) {
                length += downloadTask.blockSofar();
            }
            return length;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onCancel(IDownloadRequest task) {
        listenerDisposer.onCancel(task);
    }

    @Override
    public void onRetry(IDownloadRequest request) {
        listenerDisposer.onRetry(request);
    }

    public void onFailure(IDownloadRequest task, Throwable e) {
        listenerDisposer.onFailure(task, e);
        //计数器
        countDownLatch.countDown();
    }

    public void onComplete(IDownloadRequest task) {
        success.getAndIncrement();
        if (success.get() == blockCount) {
            if (!progressDisposer.isIgnoredProgress()) {
                listenerDisposer.onProgress(task, 1, 0, getSofarLength());
            }
            if (!speedDisposer.isIgnoredSpeed()) {
                speedDisposer.onSpeed(task, speedLength.get());
            }
            speedLength.set(0);
            try {
                M3U8Utils.mergeM3u8(request, saveFile, m3U8Info);
                listenerDisposer.onComplete(task);
            } catch (Exception e) {
                listenerDisposer.onFailure(task, e);
            }
        } else if (success.get() > blockCount){
            listenerDisposer.onFailure(task, new RuntimeException("The downloaded ts is missing!"));
        }
        XDownload.get().removeDownload(task.tag());
        //计数器
        countDownLatch.countDown();
    }


}
