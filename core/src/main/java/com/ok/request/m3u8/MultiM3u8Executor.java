package com.ok.request.m3u8;


import com.ok.request.CoreDownload;
import com.ok.request.base.DownloadExecutor;
import com.ok.request.call.RequestCall;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.down.MultiDownloadBlock;
import com.ok.request.exception.HttpErrorException;
import com.ok.request.exception.TimeoutException;
import com.ok.request.executor.AutoRetryExecutor;
import com.ok.request.factory.SerializeFactory;
import com.ok.request.info.M3U8Info;
import com.ok.request.info.M3U8Ts;
import com.ok.request.request.HttpResponse;
import com.ok.request.request.Request;
import com.ok.request.request.Response;
import com.ok.request.tool.XDownUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLProtocolException;

final class MultiM3u8Executor extends AutoRetryExecutor implements MultiDownloadBlock, DownloadExecutor {
    private final MultiM3u8Disposer multiDisposer;
    private final OkDownloadRequest httpRequest;
    private final M3U8Ts m3U8Ts;
    private final M3U8Info m3U8Info;
    private final File saveFile;
    private final File tempFile;

    private final AtomicLong sofarSeg = new AtomicLong(0);
    private final AtomicLong sofarTs = new AtomicLong(0);
    private final AtomicReference<String> currentURL = new AtomicReference<>();
    private final ReentrantLock lock;

    private final int byteArraySize;
    private final RequestCall requestCall = new RequestCall();

    public MultiM3u8Executor(
            OkDownloadRequest request,
            File saveFile,
            M3U8Info m3U8Info,
            File tempDir,
            final int index,
            MultiM3u8Disposer listener,
            ReentrantLock lock) {
        super(request);
        CoreDownload.addExecute(request.getTag(), this);

        this.httpRequest = request;
        this.saveFile = saveFile;
        this.m3U8Info = m3U8Info;
        this.m3U8Ts = m3U8Info.getTsList().get(index);
        this.tempFile = m3U8Ts.getTsFile(tempDir);
        this.multiDisposer = listener;
        this.lock = lock;
        this.byteArraySize = Math.max(2048, request.getBufferedSize());

        requestCall.setNetworkInterceptors(httpRequest.getNetworkInterceptor());
        requestCall.setInterceptors(httpRequest.getInterceptor());
    }

    @Override
    protected void onError(Throwable e) {
        if (e instanceof SSLProtocolException) {
            super.onError(new TimeoutException("Read timed out:" + currentURL.get(), e));
        } else if (e instanceof SocketTimeoutException) {
            super.onError(new TimeoutException("connect timed out:" + currentURL.get(), e));
        } else {
            super.onError(e);
        }
    }

    @Override
    protected void completeRun() {
        multiDisposer.removeTask(this);
        multiDisposer.onFinish();
        requestCall.terminated();
        CoreDownload.removeExecute(tag(), this);
    }

    @Override
    protected void applyCancel() {
        requestCall.cancel();
    }

    @Override
    protected void onExecute() throws Throwable {
        sofarSeg.set(0);
        sofarTs.set(0);

        File tempCacheDir = XDownUtils.getTempCacheDir(httpRequest);

        if (m3U8Ts.hasKey()) {
            //下载Key信息
            File keyFile = m3U8Ts.getKeyFile(tempCacheDir);
            if (!keyFile.exists() || keyFile.length() == 0) {
                downloadKey(requestCall, m3U8Ts.getKeyUri(), keyFile);
            }
        }

        if (m3U8Ts.hasInitSegment()) {
            //下载MAP片段信息
            File tsInitSegmentFile = m3U8Ts.getInitSegmentFile(tempCacheDir);
            //先获取保存的长度
            long length = m3U8Ts.getInitSegmentLength();
            downloadTs(requestCall, m3U8Ts.getInitSegmentUri(), tsInitSegmentFile, false, length);
        }

        //先获取保存的长度
        long length = m3U8Ts.getTsSize();
        File tempM3u8 = m3U8Ts.getTsFile(tempCacheDir);
        downloadTs(requestCall, m3U8Ts.getUrl(), tempM3u8, true, length);

        multiDisposer.onComplete(this);
    }

    private void downloadKey(RequestCall call, String url, File file) throws Throwable {
        checkIsCancel();
        currentURL.set(url);
        Request request = this.httpRequest.request(url);
        //开启请求
        Response response = call.process(request);
        if (!response.isSuccess()) {
            throw new HttpErrorException(response.code(), url, new HttpResponse(response).error());
        }
        file.deleteOnExit();
        //写入文件
        writeFile(response.body().source(), file, false);
    }

    /**
     * 下载m3u8片段
     *
     * @param file
     * @param url
     * @throws Exception
     */
    private void downloadTs(RequestCall call, String url, File file, boolean isTs, final long length) throws Throwable {
        checkIsCancel();

        final long start = XDownUtils.getFileExistsLength(file, length);
        if (length > 0 && start == length) return;

        currentURL.set(url);
        Request request = this.httpRequest.request(url);
        if (start > 0) {
            request.addHeader("Range", XDownUtils.jsonString("bytes=", start, "-", length));
        }
        checkIsCancel();
        //开启请求
        Response response = call.process(request);
        if (!response.isSuccess()) {
            throw new HttpErrorException(response.code(), url, new HttpResponse(response).error());
        }
        final long contentLength = response.body().contentLength();

        if (!XDownUtils.isAcceptRanges(response.headers())) {
            file.deleteOnExit();
            response = call.process(this.httpRequest.request(url));
        }

        //记录信息
        if (isTs) {
            m3U8Ts.setTsSize(contentLength);
        } else {
            m3U8Ts.setInitSegmentLength(contentLength);
        }

        lock.lock();
        try {
            SerializeFactory.writeM3u8Info(httpRequest, m3U8Info);
        } finally {
            lock.unlock();
        }

        //写入文件
        writeFile(response.body().source(), file, isTs);
        if (isTs) {
            sofarTs.getAndSet(contentLength);
        } else {
            sofarSeg.getAndSet(contentLength);
        }
    }

    /**
     * 写入文件
     *
     * @param is
     * @param cacheFile
     * @param isTs
     * @throws IOException
     */
    private void writeFile(InputStream is, File cacheFile, boolean isTs) throws IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(cacheFile, true);
            byte[] bytes = new byte[byteArraySize];
            int length;
            while ((length = is.read(bytes)) > 0) {
                checkIsCancel();
                os.write(bytes, 0, length);
                os.flush();
                onProgress(length, isTs);
            }
        } finally {
            XDownUtils.closeIo(is);
            XDownUtils.closeIo(os);
        }
    }

    /**
     * 进度
     *
     * @param length
     * @param isTs
     */
    protected void onProgress(int length, boolean isTs) {
        if (isTs) {
            sofarTs.addAndGet(length);
        } else {
            sofarSeg.addAndGet(length);
        }
        multiDisposer.onProgress(this, length);
    }

    @Override
    public long blockSofar() {
        return sofarSeg.get() + sofarTs.get();
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
