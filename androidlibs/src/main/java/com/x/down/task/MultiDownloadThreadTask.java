package com.x.down.task;


import com.x.down.XDownload;
import com.x.down.base.IConnectRequest;
import com.x.down.base.IDownloadRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.made.AutoRetryRecorder;
import com.x.down.tool.XDownUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

final class MultiDownloadThreadTask extends HttpDownloadRequest implements MultiDownloadTask, IDownloadRequest, IConnectRequest {
    private final MultiDownloadDisposer multiDisposer;
    private final XDownloadRequest request;
    private final File tempFile;
    private final String saveFile;
    private final long fileLength;
    private final long blockStart;
    private final long blockEnd;
    private final AtomicLong sofar = new AtomicLong(0);
    private volatile Future taskFuture;

    public MultiDownloadThreadTask(
            XDownloadRequest request,
            File tempFile,
            String saveFile,
            AutoRetryRecorder recorder,
            long fileLength,
            long blockStart,
            long blockEnd,
            MultiDownloadDisposer listener) {
        super(recorder, request.getBufferedSize());
        this.request = request;
        this.tempFile = tempFile;
        this.saveFile = saveFile;
        this.fileLength = fileLength;
        this.blockStart = blockStart;
        this.blockEnd = blockEnd;
        this.multiDisposer = listener;
    }

    public void setTaskFuture(Future taskFuture) {
        this.taskFuture = taskFuture;
    }

    @Override
    public void run() {
        super.run();
        XDownload.get().removeMultiDownload(tag(), this);
        taskFuture = null;
    }

    @Override
    protected void onExecute() throws Throwable {
        //是否使用断点续传
        final long start;
        if (tempFile.exists()) {
            final long fileSize = tempFile.length();
            if (fileSize == fileLength) {
                sofar.set(fileLength);
                multiDisposer.removeTask(this);
                multiDisposer.onComplete(this);
                return;
            } else if (fileSize > fileLength) {
                tempFile.delete();
                start = blockStart;
                sofar.set(0);
            } else {
                start = blockStart + fileSize;
                sofar.set(fileSize);
            }
        } else {
            start = blockStart;
            sofar.set(0);
        }

        HttpURLConnection http = request.buildConnect();
        http.setRequestProperty("Range", XDownUtils.jsonString("bytes=", start, "-", blockEnd));
        http.connect();

        multiDisposer.onConnecting(this, getHeaders(http));

        int responseCode = http.getResponseCode();

        while (isNeedRedirects(responseCode)) {
            http = redirectsConnect(http, request);
            responseCode = http.getResponseCode();
        }

        if (isSuccess(responseCode)) {
            tempFile.getParentFile().mkdirs();
            FileOutputStream os = new FileOutputStream(tempFile, true);
            if (!readInputStream(http.getInputStream(), os)) {
                return;
            }
            sofar.set(fileLength);
            multiDisposer.removeTask(this);
            multiDisposer.onComplete(this);

            XDownUtils.disconnectHttp(http);
        } else {
            String stream = readStringStream(http.getErrorStream(), XDownUtils.getInputCharset(http));
            multiDisposer.onRequestError(this, responseCode, stream);

            XDownUtils.disconnectHttp(http);
            retryToRun(responseCode,stream);
        }
    }

    @Override
    protected void onRetry() {
        multiDisposer.onRetry(this);
    }

    @Override
    protected void onError(Throwable e) {
        multiDisposer.onFailure(this, e);
    }

    @Override
    protected void onCancel() {
        multiDisposer.onCancel(this);
    }

    @Override
    protected void onProgress(int length) {
        sofar.addAndGet(length);
        multiDisposer.onProgress(this, length);
    }

    @Override
    public String getFilePath() {
        return saveFile;
    }

    @Override
    public String tag() {
        return request.getTag();
    }

    @Override
    public String url() {
        return request.getConnectUrl();
    }

    @Override
    public boolean cancel() {
        isCancel = true;
        if (taskFuture != null) {
            return taskFuture.cancel(true);
        }
        return false;
    }

    @Override
    public int retryCount() {
        return autoRetryRecorder.getRetryCount();
    }

    @Override
    public XDownloadRequest request() {
        return request;
    }

    @Override
    public long blockSofar() {
        return sofar.get();
    }

    @Override
    public File blockFile() {
        return tempFile;
    }
}
