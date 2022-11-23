package com.x.down.task;


import com.x.down.XDownload;
import com.x.down.base.IConnectRequest;
import com.x.down.base.IDownloadRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.impl.DownloadListenerDisposer;
import com.x.down.impl.ProgressDisposer;
import com.x.down.impl.SpeedDisposer;
import com.x.down.listener.OnMergeFileListener;
import com.x.down.made.AutoRetryRecorder;
import com.x.down.made.DownInfo;
import com.x.down.proxy.SerializeProxy;
import com.x.down.tool.XDownUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;

final class SingleDownloadThreadTask extends HttpDownloadRequest implements IDownloadRequest, IConnectRequest {
    private final DownloadListenerDisposer listenerDisposer;
    private final ProgressDisposer progressDisposer;
    private final SpeedDisposer speedDisposer;
    private final XDownloadRequest request;
    private volatile long sTotalLength;
    private volatile long sSofarLength = 0;
    private volatile Future taskFuture;
    private volatile int speedLength = 0;

    public SingleDownloadThreadTask(XDownloadRequest request, DownloadListenerDisposer listener, long contentLength) {
        super(new AutoRetryRecorder(request.isUseAutoRetry(),
                request.getAutoRetryTimes(),
                request.getAutoRetryInterval()), request.getBufferedSize());
        this.request = request;
        this.sTotalLength = contentLength;
        this.listenerDisposer = listener;
        this.progressDisposer = new ProgressDisposer(request.isIgnoredProgress(),
                request.getUpdateProgressTimes(),
                listener);
        this.speedDisposer = new SpeedDisposer(request.isIgnoredSpeed(), request.getUpdateSpeedTimes(), listener);
    }

    public void setTaskFuture(Future taskFuture) {
        this.taskFuture = taskFuture;
    }

    /**
     * 检测是否已经下载完成
     *
     * @return
     */
    public boolean checkComplete() {
        File file = getFile();
        if (sTotalLength > 0 && file.exists()) {
            if (file.length() == sTotalLength) {
                listenerDisposer.onComplete(this);
                return true;
            } else {
                file.delete();
            }
        }
        return false;
    }

    @Override
    public void run() {
        super.run();
        XDownload.get().removeDownload(request.getTag());
        taskFuture = null;
    }

    @Override
    protected void onConnecting(HttpURLConnection http, long length) {
        sTotalLength = length;
        listenerDisposer.onConnecting(this, getHeaders(http));
    }

    @Override
    protected void onExecute() throws Exception {
        if (checkComplete()) {
            return;
        }
        HttpURLConnection http = null;
        if (sTotalLength <= 0) {
            DownInfo info = SerializeProxy.readDownloaderInfo(request);
            if (info != null) {
                sTotalLength = info.getLength();
            }
        }

        if (sTotalLength <= 0) {
            //重新获取下载长度
            http = getDownloaderLong(request);
            //获取是否支持断点下载
            final int code = http.getResponseCode();
            if (!isSuccess(code)) {
                //获取错误信息
                String stream = readStringStream(http.getErrorStream(), XDownUtils.getInputCharset(http));
                listenerDisposer.onRequestError(this, code, stream);
                //断开请求
                XDownUtils.disconnectHttp(http);
                //重试
                retryToRun(code, stream);
                return;
            }
        }
        if (checkComplete()) {
            //断开请求
            XDownUtils.disconnectHttp(http);
            return;
        }
        final boolean isBreakPointResume;//是否断点续传

        File cacheFile = XDownUtils.getTempFile(request);

        //判断之前下载的文件是否存在或完成
        if (cacheFile.exists()) {
            if (sTotalLength > 0) {
                if (cacheFile.length() == sTotalLength) {
                    //长度一致
                    sSofarLength = sTotalLength;
                    //复制临时文件到保存文件中
                    cacheFile.renameTo(getFile());
                    //下载完成
                    speedLength = 0;
                    listenerDisposer.onComplete(this);
                    return;
                } else if (cacheFile.length() > sTotalLength) {
                    //长度大了
                    cacheFile.delete();
                    sSofarLength = 0;
                    isBreakPointResume = false;
                } else {
                    sSofarLength = cacheFile.length();
                    isBreakPointResume = request.isUseBreakpointResume();
                }
            } else {
                //获取不到文件长度
                cacheFile.delete();
                sSofarLength = 0;
                isBreakPointResume = false;
            }
        } else {
            sSofarLength = 0;
            isBreakPointResume = false;
        }

        if (isBreakPointResume) {
            //断点下载
            if (sSofarLength > 0) {
                XDownUtils.disconnectHttp(http);
                http = null;
            }
            if (http == null) {
                http = request.buildConnect();
                final long start = sSofarLength;
                //判断是否支持断点下载
                http.setRequestProperty("Range", XDownUtils.jsonString("bytes=", start, "-", sTotalLength));
                //重新连接
                http.connect();
            }
        } else {
            if (http == null) {
                http = request.buildConnect();
                http.connect();
            }
        }

        int responseCode = http.getResponseCode();

        //判断是否需要重定向
        while (isNeedRedirects(responseCode)) {
            http = redirectsConnect(http, request);
            responseCode = http.getResponseCode();
        }

        //是否支持断点下载
        boolean acceptRanges = isAcceptRanges(http);

        //重新判断
        if (!isSuccess(responseCode)) {
            onResponseError(http, responseCode);
            return;
        }
        cacheFile.getParentFile().mkdirs();
        //重新下载
        if (!downReadInput(http, cacheFile, acceptRanges && isBreakPointResume)) {
            return;
        }
        //复制下载完成的文件
        cacheFile.renameTo(getFile());

        //处理最后的进度
        if (!progressDisposer.isIgnoredProgress()) {
            listenerDisposer.onProgress(this, 1, sTotalLength, sTotalLength);
        }
        //处理最后的速度
        if (!speedDisposer.isIgnoredSpeed()) {
            speedDisposer.onSpeed(this, speedLength);
        }
        OnMergeFileListener listener = request.getOnMegerFileListener();
        if (listener != null) {
            listener.onMerge(getFile());
        }
        speedLength = 0;
        //完成回调
        listenerDisposer.onComplete(this);
    }

    private boolean downReadInput(HttpURLConnection http, File cacheFile, boolean append) throws IOException {
        if (!append) {
            sSofarLength = 0;
        }
        try {
            FileOutputStream os = new FileOutputStream(cacheFile, append);
            return readInputStream(http.getInputStream(), os);
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
    private void onResponseError(HttpURLConnection http, int responseCode) throws IOException {
        String stream = readStringStream(http.getErrorStream(), XDownUtils.getInputCharset(http));
        listenerDisposer.onRequestError(this, responseCode, stream);

        XDownUtils.disconnectHttp(http);
        retryToRun(responseCode, stream);
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
        sSofarLength += length;
        speedLength += length;
        if (progressDisposer.isCallProgress()) {
            progressDisposer.onProgress(this, sTotalLength, sSofarLength);
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
}
