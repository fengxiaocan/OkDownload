package com.ok.request.m3u8;


import com.ok.request.base.DownloadExecutor;
import com.ok.request.call.RequestCall;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.disposer.ProgressDisposer;
import com.ok.request.disposer.SpeedDisposer;
import com.ok.request.exception.CancelTaskException;
import com.ok.request.exception.HttpErrorException;
import com.ok.request.factory.SerializeFactory;
import com.ok.request.info.M3U8Info;
import com.ok.request.info.M3U8Ts;
import com.ok.request.request.HttpResponse;
import com.ok.request.request.Request;
import com.ok.request.request.Response;
import com.ok.request.tool.M3U8Utils;
import com.ok.request.tool.XDownUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

final class SingleDownloadM3u8 {
    private final int byteArraySize;
    private final OkDownloadRequest httpRequest;
    private final M3U8Info m3U8Info;
    private final DownloadExecutor downloadExecutor;
    private final ProgressDisposer progressDisposer;
    private final SpeedDisposer speedDisposer;
    private final AtomicBoolean cancelAtomic = new AtomicBoolean(false);
    private volatile int downloadIndex = 0;
    private volatile int speedLength = 0;
    private volatile long sofarLength = 0;
    private volatile long currentLength = 0;

    public SingleDownloadM3u8(OkDownloadRequest request, DownloadExecutor executor, M3U8Info m3U8Info) {
        this.httpRequest = request;
        this.downloadExecutor = executor;
        this.m3U8Info = m3U8Info;
        this.progressDisposer = new ProgressDisposer(request.isIgnoredProgress(), request.getUpdateProgressTimes(), request);
        this.speedDisposer = new SpeedDisposer(request.isIgnoredSpeed(), request.getUpdateSpeedTimes(), request);
        this.byteArraySize = Math.max(2048, request.getBufferedSize());
    }

    public void onExecute(RequestCall call) throws Throwable {
        //判断之前下载的文件是否存在或完成
        File tempCacheDir = XDownUtils.getTempCacheDir(httpRequest);
        if (!httpRequest.isUseBreakpointResume()) {
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
                if (!keyFile.exists() || keyFile.length() == 0) {
                    downloadKey(call, block.getKeyUri(), keyFile);
                }
            }

            if (block.hasInitSegment()) {
                //下载MAP片段信息
                File tsInitSegmentFile = block.getInitSegmentFile(tempCacheDir);
                //先获取保存的长度
                long length = block.getInitSegmentLength();
                downloadTs(call, block.getInitSegmentUri(), tsInitSegmentFile, block, false, length);
            }

            //先获取保存的长度
            long length = block.getTsSize();
            File tempM3u8 = block.getTsFile(tempCacheDir);
            downloadTs(call, block.getUrl(), tempM3u8, block, true, length);
        }

        //处理最后的进度
        if (!progressDisposer.isIgnoredProgress()) {
            progressDisposer.onProgress(downloadExecutor, 1, sofarLength, sofarLength);
        }
        //处理最后的速度
        if (!speedDisposer.isIgnoredSpeed()) {
            speedDisposer.onSpeed(downloadExecutor, speedLength);
        }
        speedLength = 0;

        if (M3U8Utils.mergeM3u8(downloadExecutor, httpRequest, httpRequest.getSaveFile(), m3U8Info)) {
            //完成回调
            httpRequest.callDownloadComplete(downloadExecutor);
        }
    }

    /**
     * 下载key文件
     *
     * @param call
     * @param url
     * @param file
     * @throws Throwable
     */
    private void downloadKey(RequestCall call, String url, File file) throws Throwable {
        checkIsCancel();

        ((M3u8DownloadExecutor) downloadExecutor).currentURL.set(url);
        Request request = this.httpRequest.request(url);
        //开启请求
        Response response = call.process(request);
        if (!response.isSuccess()) {
            throw new HttpErrorException(response.code(), url, new HttpResponse(response).error());
        }
        file.deleteOnExit();
        //写入文件
        writeFile(response.body().source(), file);
    }

    /**
     * 下载m3u8片段
     *
     * @param file
     * @param url
     * @throws Exception
     */
    private void downloadTs(RequestCall call, String url, File file, M3U8Ts block, boolean isTs, final long length) throws Throwable {
        checkIsCancel();

        final long start = XDownUtils.getFileExistsLength(file, length);
        if (length > 0 && start == length) return;

        currentLength = start;

        ((M3u8DownloadExecutor) downloadExecutor).currentURL.set(url);
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
            if (!response.isSuccess()) {
                throw new HttpErrorException(response.code(), url, new HttpResponse(response).error());
            }
        }
        //记录信息
        if (block != null) {
            if (isTs) {
                block.setTsSize(contentLength);
            } else {
                block.setInitSegmentLength(contentLength);
            }
            SerializeFactory.writeM3u8Info(httpRequest, m3U8Info);
        }
        //写入文件
        writeFile(response.body().source(), file);
        currentLength = 0;
        sofarLength += contentLength;
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

    private void onProgress(final int length) {
        currentLength += length;
        speedLength += length;
        if (progressDisposer.isCallProgress()) {
            int size = m3U8Info.getTsList().size();
            float v = downloadIndex * 1F / size;
            progressDisposer.onProgress(downloadExecutor, v, 0, currentLength + sofarLength);
        }
        if (speedDisposer.isCallSpeed()) {
            speedDisposer.onSpeed(downloadExecutor, speedLength);
            speedLength = 0;
        }
    }

    public void cancel() {
        cancelAtomic.getAndSet(true);
    }

    private void checkIsCancel() {
        if (cancelAtomic.get()) {
            throw new CancelTaskException();
        }
    }
}
