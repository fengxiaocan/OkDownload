package com.x.down.task;


import com.x.down.XDownload;
import com.x.down.base.IConnectRequest;
import com.x.down.base.IDownloadRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.impl.DownloadListenerDisposer;
import com.x.down.impl.ProgressDisposer;
import com.x.down.impl.SpeedDisposer;
import com.x.down.m3u8.M3U8Info;
import com.x.down.m3u8.M3U8Ts;
import com.x.down.m3u8.M3U8Utils;
import com.x.down.made.AutoRetryRecorder;
import com.x.down.proxy.SerializeProxy;
import com.x.down.tool.XDownUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;

final class SingleDownloadM3u8Task extends HttpDownloadRequest implements IDownloadRequest, IConnectRequest {
    private final DownloadListenerDisposer listenerDisposer;
    private final ProgressDisposer progressDisposer;
    private final SpeedDisposer speedDisposer;
    private final M3U8Info m3U8Info;
    private final XDownloadRequest request;
    private volatile int downloadIndex = 0;
    private volatile Future taskFuture;
    private volatile int speedLength = 0;
    private volatile long sofarLength = 0;
    private volatile long currentLength = 0;

    public SingleDownloadM3u8Task(XDownloadRequest request, DownloadListenerDisposer listener, M3U8Info m3U8Info) {
        super(new AutoRetryRecorder(request.isUseAutoRetry(),
                request.getAutoRetryTimes(),
                request.getAutoRetryInterval()), request.getBufferedSize());
        this.request = request;
        this.m3U8Info = m3U8Info;
        this.listenerDisposer = listener;
        this.progressDisposer = new ProgressDisposer(request.isIgnoredProgress(),
                request.getUpdateProgressTimes(),
                listener);
        this.speedDisposer = new SpeedDisposer(request.isIgnoredSpeed(), request.getUpdateSpeedTimes(), listener);
    }

    public void setTaskFuture(Future taskFuture) {
        this.taskFuture = taskFuture;
    }

    @Override
    protected void completeRun() {
        XDownload.get().removeDownload(tag());
        taskFuture = null;
    }

    @Override
    protected void onExecute() throws Exception {
        //判断之前下载的文件是否存在或完成
        File tempCacheDir = XDownUtils.getTempCacheDir(request);
        if (!request.isUseBreakpointResume()) {
            XDownUtils.deleteDir(tempCacheDir);
        }
        tempCacheDir.mkdirs();
        sofarLength = 0;

        checkIsCancel();

        for (int i = 0; i < m3U8Info.getTsList().size(); i++) {
            downloadIndex = i;
            M3U8Ts block = m3U8Info.getTsList().get(i);

            checkIsCancel();

            if (block.hasKey()) {
                //下载Key信息
                File keyFile = block.getKeyFile(tempCacheDir);
                checkIsCancel();
                downloadTs(keyFile, block.getKeyUri(), 0);
            }

            if (block.hasInitSegment()) {
                //下载MAP片段信息
                File tsInitSegmentFile = block.getInitSegmentFile(tempCacheDir);
                //先获取保存的长度
                long length = block.getInitSegmentLength();
                if (length <= 0) {
                    length = downloadLong(block.getInitSegmentUri());
                    block.setInitSegmentLength(length);
                    //更新长度保存到本地
                    SerializeProxy.writeM3u8Info(request, m3U8Info);
                }
                checkIsCancel();

                downloadTs(tsInitSegmentFile, block.getInitSegmentUri(), length);
            }

            //先获取保存的长度
            long length = block.getTsSize();
            if (length <= 0) {
                length = downloadLong(block.getUrl());
                block.setTsSize(length);
                //更新长度保存到本地
                SerializeProxy.writeM3u8Info(request, m3U8Info);
            }
            File tempM3u8 = block.getTsFile(tempCacheDir);
            downloadTs(tempM3u8, block.getUrl(), length);
        }

        M3U8Utils.mergeM3u8(request, getFile(), m3U8Info);

        //处理最后的进度
        if (!progressDisposer.isIgnoredProgress()) {
            listenerDisposer.onProgress(this, 1, sofarLength, sofarLength);
        }
        //处理最后的速度
        if (!speedDisposer.isIgnoredSpeed()) {
            speedDisposer.onSpeed(this, speedLength);
        }
        speedLength = 0;
        //完成回调
        listenerDisposer.onComplete(this);
    }


    /**
     * 下载m3u8片段
     *
     * @param file
     * @param url
     * @throws Exception
     */
    private void downloadTs(File file, String url, long length) throws Exception {
        checkIsCancel();

        long start = 0;
        if (file.exists()) {
            if (file.length() > 0 && file.length() == length) {
                return;
            } else if (file.length() > length) {
                file.delete();
                start = 0;
            } else {
                start = file.length();
            }
        }
        currentLength = start;
        HttpURLConnection http = request.buildConnect(url);
        if (start > 0) {
            http.setRequestProperty("Range", XDownUtils.jsonString("bytes=", start, "-", length));
        }
        http.connect();

        checkIsCancel();

        listenerDisposer.onConnecting(this, getHeaders(http));
        int responseCode = http.getResponseCode();
        //是否支持断点下载
        boolean acceptRanges = isAcceptRanges(http);
        //判断是否成功
        if (!isSuccess(responseCode)) {
            onResponseError(http, responseCode);
            return;
        }

        //重新下载
        downReadInput(http, file, acceptRanges);

        sofarLength += length;
        currentLength = 0;
    }

    /**
     * 获取片段的长度
     *
     * @throws Exception
     */
    private long downloadLong(String url) throws Exception {
        HttpURLConnection http = request.buildConnect(url);
        checkIsCancel();

        int responseCode = http.getResponseCode();

        while (isNeedRedirects(responseCode)) {
            http = redirectsConnect(http, request);
            responseCode = http.getResponseCode();
        }

        checkIsCancel();

        //优先获取文件长度再回调
        long contentLength = XDownUtils.getContentLength(http);

        //连接中
        listenerDisposer.onConnecting(this, getHeaders(http));

        if (contentLength <= 0) {
            //长度获取不到的时候重新连接 获取不到长度则要求http请求不要gzip压缩
            XDownUtils.disconnectHttp(http);
            http = request.buildConnect();
            http.setRequestProperty("Accept-Encoding", "identity");
            http.connect();

            contentLength = XDownUtils.getContentLength(http);
            //连接中
        }

        checkIsCancel();

        listenerDisposer.onConnecting(this, getHeaders(http));
        XDownUtils.disconnectHttp(http);
        return contentLength;
    }

    /**
     * @param http
     * @param file
     * @param isAppend
     * @return true 为完成操作,false为取消操作,需要退出循环
     * @throws IOException
     */
    private void downReadInput(HttpURLConnection http, File file, boolean isAppend) throws IOException {
        if (!isAppend) {
            currentLength = 0;
        }
        try {
            FileOutputStream os = new FileOutputStream(file, isAppend);
            readInputStream(http.getInputStream(), os);
        } finally {
            XDownUtils.disconnectHttp(http);
        }
    }

    /**
     * 处理失败的回调
     *
     * @param http
     * @param responseCode
     * @throws IOException
     */
    private void onResponseError(HttpURLConnection http, int responseCode) throws Exception {
        String stream = readStringStream(http.getErrorStream(), XDownUtils.getInputCharset(http));
        listenerDisposer.onRequestError(this, responseCode, stream);

        XDownUtils.disconnectHttp(http);
        tryToRetry(responseCode, stream);
    }

    @Override
    protected void onRetry() {
        listenerDisposer.onRetry(this);
    }

    @Override
    protected void onError(Throwable e) {
        listenerDisposer.onFailure(this, e);
    }

    @Override
    protected void onCancel() {
        listenerDisposer.onCancel(this);
    }

    @Override
    protected void onProgress(int length) {
        currentLength += length;
        speedLength += length;
        if (progressDisposer.isCallProgress()) {
            int size = m3U8Info.getTsList().size();
            float v = downloadIndex * 1F / size;
            progressDisposer.onProgress(this, v, 0, currentLength + sofarLength);
        }
        if (speedDisposer.isCallSpeed()) {
            speedDisposer.onSpeed(this, speedLength);
            speedLength = 0;
        }
    }

    private File getFile() {
        return request.getSaveFile();
    }

    @Override
    public String getFilePath() {
        return getFile().getAbsolutePath();
    }

    @Override
    public String tag() {
        return request.getIdentifier();
    }

    @Override
    public String url() {
        return request.getConnectUrl();
    }

    @Override
    public boolean cancel() {
       cancelTask();
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
}
