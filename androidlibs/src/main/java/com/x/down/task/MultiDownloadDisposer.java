package com.x.down.task;

import com.x.down.XDownload;
import com.x.down.base.IDownloadRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.data.Headers;
import com.x.down.impl.DownloadListenerDisposer;
import com.x.down.impl.ProgressDisposer;
import com.x.down.impl.SpeedDisposer;
import com.x.down.listener.OnDownloadConnectListener;
import com.x.down.listener.OnMergeFileListener;
import com.x.down.tool.XDownUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

final class MultiDownloadDisposer implements OnDownloadConnectListener {

    private final XDownloadRequest request;
    private final int blockCount;
    private final DownloadListenerDisposer listenerDisposer;
    private final ProgressDisposer progressDisposer;
    private final SpeedDisposer speedDisposer;
    private final LinkedList<MultiDownloadTask> taskList = new LinkedList<>();
    private final LinkedList<File> tempFileList = new LinkedList<>();
    private final CountDownLatch countDownLatch;
    private final AtomicInteger success = new AtomicInteger(0);
    private final AtomicLong sofar = new AtomicLong(0);
    private final AtomicInteger speedLength = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();
    private final long totalLength;

    public MultiDownloadDisposer(XDownloadRequest request, CountDownLatch countDownLatch,
                                 int blockCount, DownloadListenerDisposer disposer, long totalLength) {
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
        this.totalLength = totalLength;
    }

    public void addTask(MultiDownloadTask task) {
        taskList.add(task);
        tempFileList.add(task.blockFile());
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
            progressDisposer.onProgress(request, totalLength, getSofarLength());
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

    public synchronized void onFailure(IDownloadRequest task, Throwable exception) {
        listenerDisposer.onFailure(task, exception);
        //计数器
        countDownLatch.countDown();
    }

    public synchronized void onComplete(IDownloadRequest task) {
        success.getAndIncrement();

        if (success.get() == blockCount) {
            if (!progressDisposer.isIgnoredProgress()) {
                listenerDisposer.onProgress(task, 1, totalLength, totalLength);
            }

            if (!speedDisposer.isIgnoredSpeed()) {
                speedDisposer.onSpeed(task, speedLength.get());
            }
            speedLength.set(0);

            File file = task.request().getSaveFile();
            file.getParentFile().mkdirs();

            byte[] bytes = new byte[1024 * 8];
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                for (File tempFile : tempFileList) {
                    FileInputStream inputStream = null;
                    try {
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
                OnMergeFileListener listener = task.request().getOnMegerFileListener();
                if (listener != null) {
                    listener.onMerge(file);
                }
                listenerDisposer.onComplete(task);
            } catch (Exception e) {
                listenerDisposer.onFailure(task, e);
            } finally {
                XDownUtils.closeIo(outputStream);
            }
        } else {
            listenerDisposer.onFailure(task, new RuntimeException("The downloaded ts is missing!"));
        }
        XDownload.get().removeDownload(request.getTag());
        taskList.clear();
        countDownLatch.countDown();
    }
}
