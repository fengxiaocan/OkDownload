package com.x.down.task;


import com.x.down.base.IConnectRequest;
import com.x.down.base.MultiDownloadTask;
import com.x.down.core.XDownloadRequest;
import com.x.down.impl.MultiDisposer;
import com.x.down.made.AutoRetryRecorder;
import com.x.down.tool.XDownUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;

final class MultiDownloadThreadTask extends HttpDownloadRequest implements MultiDownloadTask, IConnectRequest {
    private final MultiDisposer multiDisposer;
    private final XDownloadRequest request;
    private final File tempFile;
    private final String saveFile;
    private final int index;
    private final long contentLength;
    private final long fileLength;
    private final long blockStart;
    private final long blockEnd;
    private volatile Future taskFuture;

    public MultiDownloadThreadTask(
            XDownloadRequest request,
            File tempFile,
            String saveFile,
            AutoRetryRecorder recorder,
            int index,
            long contentLength,
            long fileLength,
            long blockStart,
            long blockEnd,
            MultiDisposer listener) {
        super(recorder, request.getBufferedSize());
        this.request = request;
        this.tempFile = tempFile;
        this.saveFile = saveFile;
        this.index = index;
        this.contentLength = contentLength;
        this.fileLength = fileLength;
        this.blockStart = blockStart;
        this.blockEnd = blockEnd;
        this.multiDisposer = listener;
        multiDisposer.onPending(this);
    }

    public final void setTaskFuture(Future taskFuture) {
        this.taskFuture = taskFuture;
    }

    @Override
    public void run() {
        multiDisposer.onStart(this);
        super.run();
    }

    @Override
    protected void onExecute() throws Throwable {
        //是否使用断点续传
        final long start;
        if (tempFile.exists()) {
            final long fileLenght = tempFile.length();
            if (fileLenght == fileLength) {
                multiDisposer.onComplete(this);
                return;
            } else if (fileLenght > fileLength) {
                tempFile.delete();
                start = blockStart;
            } else {
                start = blockStart + fileLenght;
            }
        } else {
            start = blockStart;
        }

        HttpURLConnection http = request.buildConnect();
        http.setRequestProperty("Range", XDownUtils.jsonString("bytes=", start, "-", blockEnd));
        http.connect();

        multiDisposer.onConnecting(this);

        int responseCode = http.getResponseCode();

        while (isNeedRedirects(responseCode)) {
            http = redirectsConnect(http, request);
            responseCode = http.getResponseCode();
        }

        if (isSuccess(responseCode)) {
            FileOutputStream os = new FileOutputStream(tempFile, true);
            if (!readInputStream(http.getInputStream(), os)) {
                return;
            }
            multiDisposer.onComplete(this);

            XDownUtils.disconnectHttp(http);
        } else {
            String stream = readStringStream(http.getErrorStream(), XDownUtils.getInputCharset(http));
            multiDisposer.onRequestError(this, responseCode, stream);

            XDownUtils.disconnectHttp(http);
            retryToRun();
        }
    }

    @Override
    protected void onRetry() {
        multiDisposer.onRetry(this);
    }

    @Override
    protected void onError(Throwable e) {
        multiDisposer.onFailure(this);
    }

    @Override
    protected void onCancel() {
        multiDisposer.onCancel(this);
    }

    @Override
    protected void onProgress(int length) {
        multiDisposer.onProgress(this, contentLength, length);
    }

    @Override
    public int blockIndex() {
        return index;
    }

    @Override
    public String getFilePath() {
        return saveFile;
    }

    @Override
    public long getTotalLength() {
        return contentLength;
    }

    @Override
    public long getSofarLength() {
        return multiDisposer.getSofarLength();
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
    public long blockStart() {
        return blockStart;
    }

    @Override
    public long blockEnd() {
        return blockEnd;
    }

    @Override
    public long blockSofarLength() {
        return tempFile.length();
    }

    @Override
    public File blockFile() {
        return tempFile;
    }
}
