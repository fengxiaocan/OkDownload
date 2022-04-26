package com.x.down.task;


import com.x.down.base.IConnectRequest;
import com.x.down.base.MultiDownloadTask;
import com.x.down.core.XDownloadRequest;
import com.x.down.made.AutoRetryRecorder;
import com.x.down.made.M3u8DownloaderBlock;
import com.x.down.tool.XDownUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;

final class MultiDownloadM3u8Task extends HttpDownloadRequest implements MultiDownloadTask, IConnectRequest {
    private final MultiM3u8Disposer multiDisposer;
    private final XDownloadRequest request;
    private final M3u8DownloaderBlock m3U8DownloaderBlock;
    private final String saveFile;
    private final File tempFile;
    private final int index;
    private volatile Future taskFuture;

    public MultiDownloadM3u8Task(
            XDownloadRequest request,
            String saveFile,
            M3u8DownloaderBlock m3U8DownloaderBlock,
            File tempFile,
            AutoRetryRecorder recorder,
            int index,
            MultiM3u8Disposer listener) {
        super(recorder, request.getBufferedSize());
        this.request = request;
        this.saveFile = saveFile;
        this.m3U8DownloaderBlock = m3U8DownloaderBlock;
        this.tempFile = tempFile;
        this.index = index;
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
        long start = 0;
        long length = InfoSerializeProxy.readM3u8DownloaderBlock(request, m3U8DownloaderBlock);

        if (length <= 0) {
            length = downloadLong(m3U8DownloaderBlock);
            InfoSerializeProxy.writeM3u8DownloaderBlock(request, m3U8DownloaderBlock, length);
        }

        if (tempFile.exists()) {
            if (tempFile.length() == length) {
                multiDisposer.onComplete(this);
                return;
            } else if (tempFile.length() > length) {
                tempFile.delete();
                start = 0;
            } else {
                start = tempFile.length();
            }
        }

        HttpURLConnection http = request.buildConnect(m3U8DownloaderBlock.getUrl());
        if (start > 0) {
            http.setRequestProperty("Range", XDownUtils.jsonString("bytes=", start, "-", length));
        }
        multiDisposer.onConnecting(this);

        int responseCode = http.getResponseCode();

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

    /**
     * 获取片段的长度
     *
     * @param block
     * @return
     * @throws Exception
     */
    private long downloadLong(M3u8DownloaderBlock block) throws Exception {
        HttpURLConnection http = request.buildConnect(block.getUrl());
        int responseCode = http.getResponseCode();

        while (isNeedRedirects(responseCode)) {
            http = redirectsConnect(http, request);
            responseCode = http.getResponseCode();
        }
        //优先获取文件长度再回调
        long contentLength = XDownUtils.getContentLength(http);

        multiDisposer.onConnecting(this);

        //连接中
        if (contentLength <= 0) {
            //长度获取不到的时候重新连接 获取不到长度则要求http请求不要gzip压缩
            XDownUtils.disconnectHttp(http);
            http = request.buildConnect();
            http.setRequestProperty("Accept-Encoding", "identity");
            http.connect();

            multiDisposer.onConnecting(this);

            contentLength = XDownUtils.getContentLength(http);
            //连接中
        }
        XDownUtils.disconnectHttp(http);
        return contentLength;
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
        multiDisposer.onProgress(this, length);
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
        return 0;
    }

    @Override
    public long getSofarLength() {
        return 0;
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
        return 0;
    }

    @Override
    public long blockEnd() {
        return 0;
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
