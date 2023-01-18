package com.ok.request.down;


import com.ok.request.CoreDownload;
import com.ok.request.base.DownloadExecutor;
import com.ok.request.call.RequestCall;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.exception.HttpErrorException;
import com.ok.request.executor.AutoRetryExecutor;
import com.ok.request.request.HttpResponse;
import com.ok.request.request.Request;
import com.ok.request.request.Response;
import com.ok.request.tool.XDownUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

public final class MultiDownloadExecutor extends AutoRetryExecutor implements MultiDownloadBlock, DownloadExecutor {
    private final MultiDownloadDisposer multiDisposer;
    private final OkDownloadRequest request;
    private final File tempFile;
    private final File saveFile;
    private final long fileLength;
    private final long blockStart;
    private final long blockEnd;
    private final int byteArraySize;
    private final AtomicLong sofar = new AtomicLong(0);
    private final RequestCall httpCall = new RequestCall();

    public MultiDownloadExecutor(
            OkDownloadRequest request,
            File tempFile,
            File saveFile,
            long fileLength,
            long blockStart,
            long blockEnd,
            MultiDownloadDisposer listener) {
        super(request);

        CoreDownload.addExecute(request.getTag(), this);
        this.request = request;
        this.tempFile = tempFile;
        this.saveFile = saveFile;
        this.fileLength = fileLength;
        this.blockStart = blockStart;
        this.blockEnd = blockEnd;
        this.multiDisposer = listener;
        this.byteArraySize = Math.max(2048, request.getBufferedSize());

        httpCall.setNetworkInterceptors(request.getNetworkInterceptor());
        httpCall.setInterceptors(request.getInterceptor());
    }

    @Override
    protected void completeRun() {
        multiDisposer.removeTask(this);
        multiDisposer.onFinish();
        httpCall.terminated();
        CoreDownload.removeExecute(tag(), this);
    }

    @Override
    protected void applyCancel() {
        httpCall.cancel();
    }

    @Override
    protected void onExecute() throws Throwable {
        //是否使用断点续传
        final long start;
        if (tempFile.exists()) {
            final long fileSize = tempFile.length();
            if (fileSize == fileLength) {
                sofar.set(fileLength);
                multiDisposer.onComplete(this);
                return;
            } else if (fileSize > fileLength) {
                tempFile.deleteOnExit();
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

        Request request = this.request.request();
        request.addHeader("Range", XDownUtils.jsonString("bytes=", start, "-", blockEnd));

        checkIsCancel();
        Response response = httpCall.process(request);

        checkIsCancel();
        if (!response.isSuccess()) {
            HttpResponse result = new HttpResponse(response);
            String url = response.request().url().toString();
            throw new HttpErrorException(result.code(), url, result.error());
        }
        tempFile.getParentFile().mkdirs();

        //重新下载
        writeFile(response.body().source(), tempFile);
        sofar.set(fileLength);
        multiDisposer.onComplete(this);
    }

    private void writeFile(InputStream is, File cacheFile) throws IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(cacheFile, true);
            byte[] bytes = new byte[byteArraySize];
            int length;
            while ((length = is.read(bytes)) > 0) {
                checkIsCancel();
                os.write(bytes, 0, length);
                os.flush();
                onProgress(length);
            }
        } finally {
            XDownUtils.closeIo(is);
            XDownUtils.closeIo(os);
        }
    }

    private void onProgress(int length) {
        sofar.addAndGet(length);
        multiDisposer.onProgress(this, length);
    }

    @Override
    public long blockSofar() {
        return sofar.get();
    }

    @Override
    public File blockFile() {
        return tempFile;
    }

    @Override
    public File saveFile() {
        return saveFile;
    }
}
