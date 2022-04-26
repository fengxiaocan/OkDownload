package com.x.down.impl;

import com.x.down.XDownload;
import com.x.down.base.IDownloadRequest;
import com.x.down.base.MultiDownloadTask;
import com.x.down.core.XDownloadRequest;
import com.x.down.listener.OnDownloadConnectListener;
import com.x.down.tool.XDownUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

public final class MultiDisposer implements OnDownloadConnectListener {

    private final XDownloadRequest request;
    private final int blockCount;
    private final DownloadListenerDisposer listenerDisposer;
    private final ProgressDisposer progressDisposer;
    private final SpeedDisposer speedDisposer;
    private final LinkedList<MultiDownloadTask> taskList = new LinkedList<>();
    private final CountDownLatch countDownLatch;
    private volatile int successIndex = 0;//成功位置
    private volatile int bolckIndex = 0;//指针位置
    private volatile int speedLength = 0;

    public MultiDisposer(XDownloadRequest request, CountDownLatch countDownLatch, int blockCount, DownloadListenerDisposer disposer) {
        this.request = request;
        this.blockCount = blockCount;
        this.listenerDisposer = disposer;
        final boolean ignoredProgress = request.isIgnoredProgress();
        final int updateProgressTimes = request.getUpdateProgressTimes();
        this.progressDisposer = new ProgressDisposer(ignoredProgress, updateProgressTimes, disposer);
        final boolean ignoredSpeed = request.isIgnoredSpeed();
        final int updateSpeedTimes = request.getUpdateSpeedTimes();
        this.speedDisposer = new SpeedDisposer(ignoredSpeed, updateSpeedTimes, disposer);
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onPending(IDownloadRequest task) {
        listenerDisposer.onPending(task);
    }

    public void addTask(MultiDownloadTask task) {
        taskList.add(task);
    }

    @Override
    public void onStart(IDownloadRequest task) {
        listenerDisposer.onStart(task);
    }

    @Override
    public void onConnecting(IDownloadRequest request) {
        listenerDisposer.onConnecting(request);
    }

    @Override
    public void onRequestError(IDownloadRequest request, int code, String error) {
        listenerDisposer.onRequestError(request, code, error);
    }

    public void onProgress(IDownloadRequest request, long contentLength, int length) {
        speedLength += length;

        if (progressDisposer.isCallProgress()) {
            progressDisposer.onProgress(request, contentLength, getSofarLength());
        }

        if (speedDisposer.isCallSpeed()) {
            speedDisposer.onSpeed(request, speedLength);
            speedLength = 0;
        }
    }

    public long getSofarLength() {
        synchronized (Object.class) {
            long length = 0;
            for (MultiDownloadTask downloadTask : taskList) {
                length += downloadTask.blockSofarLength();
            }
            return length;
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

    public synchronized void onFailure(IDownloadRequest task) {
        bolckIndex++;
        listenerDisposer.onFailure(task);
        if (bolckIndex >= blockCount) {
            listenerDisposer.onFailure(task);
            XDownload.get().removeDownload(request.getTag());
        }
        //计数器
        countDownLatch.countDown();
    }

    public synchronized void onComplete(IDownloadRequest task) {
        bolckIndex++;
        successIndex++;

        if (bolckIndex == blockCount) {
            if (successIndex == blockCount) {
                if (!progressDisposer.isIgnoredProgress()) {
                    listenerDisposer.onProgress(task, 1);
                }

                if (!speedDisposer.isIgnoredSpeed()) {
                    speedDisposer.onSpeed(task, speedLength);
                }
                speedLength = 0;

                File file = new File(task.getFilePath());
                byte[] bytes = new byte[1024 * 8];
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                    for (MultiDownloadTask downloadTask : taskList) {
                        FileInputStream inputStream = null;
                        try {
                            File tempFile = downloadTask.blockFile();
                            inputStream = new FileInputStream(tempFile);
                            int length;
                            while ((length = inputStream.read(bytes)) > 0) {
                                outputStream.write(bytes, 0, length);
                            }
                            tempFile.delete();
                        } finally {
                            XDownUtils.closeIo(inputStream);
                        }
                    }
                    listenerDisposer.onComplete(task);
                    XDownload.get().removeDownload(request.getTag());
                } catch (Exception e) {
                    onFailure(task);
                } finally {
                    XDownUtils.closeIo(outputStream);
                }
            } else {
                onFailure(task);
            }
        }
        countDownLatch.countDown();
    }
}
