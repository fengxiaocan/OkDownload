package com.ok.request.down;


import com.ok.request.base.DownloadExecutor;
import com.ok.request.call.RequestCall;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.disposer.ProgressDisposer;
import com.ok.request.disposer.SpeedDisposer;
import com.ok.request.exception.CancelTaskException;
import com.ok.request.exception.HttpErrorException;
import com.ok.request.listener.OnDownloadListener;
import com.ok.request.request.HttpResponse;
import com.ok.request.request.Request;
import com.ok.request.request.Response;
import com.ok.request.tool.XDownUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

final class SingleDownloadConnection {
    private final int byteArraySize;
    private final OkDownloadRequest httpRequest;
    private final DownloadExecutor downloadExecutor;
    private final File saveFile;
    private final SpeedDisposer speedDisposer;
    private final ProgressDisposer progressDisposer;
    private final AtomicBoolean cancelAtomic = new AtomicBoolean(false);

    private long sSofarLength = 0;
    private long sTotalLength = 0;
    private int sSpeedLength = 0;

    public SingleDownloadConnection(OkDownloadRequest request, DownloadExecutor executor) {
        this.httpRequest = request;
        this.downloadExecutor = executor;
        this.saveFile = request.getSaveFile();
        this.progressDisposer = new ProgressDisposer(request.isIgnoredProgress(), request.getUpdateProgressTimes(), request);
        this.speedDisposer = new SpeedDisposer(request.isIgnoredSpeed(), request.getUpdateSpeedTimes(), request);
        this.byteArraySize = Math.max(2048, request.getBufferedSize());
    }

    public void onExecute(final RequestCall call, final Response response, final boolean acceptRanges) throws Throwable {
        final long contentLength = response.body().contentLength();
        onExecute(call, response, contentLength, acceptRanges);
    }

    public void onExecute(final RequestCall call, final Response response, final long contentLength, final boolean acceptRanges) throws Throwable {
        sTotalLength = contentLength;
        File cacheFile = XDownUtils.getTempFile(httpRequest);
        //判断之前下载的文件是否存在或完成
        if (cacheFile.exists()) {
            if (acceptRanges) {
                if (contentLength > 0) {
                    if (cacheFile.length() == contentLength) {
                        //长度一致
                        sSofarLength = contentLength;
                        sSpeedLength = 0;
                        //复制临时文件到保存文件中
                        cacheFile.renameTo(saveFile);
                        onComplete();
                        return;
                    } else if (cacheFile.length() > contentLength) {
                        //长度大了
                        cacheFile.deleteOnExit();
                    }
                }
            } else {
                cacheFile.deleteOnExit();
            }
        }
        sSofarLength = cacheFile.length();

        Response httpResponse = response;
        if (sSofarLength > 0) {
            //断点下载
            Request request = httpRequest.request();
            request.addHeader("Range", XDownUtils.jsonString("bytes=", sSofarLength, "-", contentLength));
            httpResponse = call.process(request);
        }
        checkIsCancel();

        if (httpResponse == null) {
            httpResponse = call.process(httpRequest.request());
        }
        checkIsCancel();

        if (!httpResponse.isSuccess()) {
            HttpResponse result = new HttpResponse(httpResponse);
            String url = response.request().url().toString();
            throw new HttpErrorException(httpResponse.code(), url, result.error());
        }
        cacheFile.getParentFile().mkdirs();

        //重新下载
        writeFile(httpResponse.body().source(), cacheFile);
        //复制下载完成的文件
        cacheFile.renameTo(saveFile);

        //处理最后的进度
        if (!progressDisposer.isIgnoredProgress()) {
            progressDisposer.onProgress(downloadExecutor, 1, contentLength, contentLength);
        }
        //处理最后的速度
        if (!speedDisposer.isIgnoredSpeed()) {
            speedDisposer.onSpeed(downloadExecutor, sSpeedLength);
        }
        sSpeedLength = 0;
        //完成回调
        onComplete();
    }

    private void writeFile(InputStream is, File cacheFile) throws IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(cacheFile,true);
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
        sSofarLength += length;
        sSpeedLength += length;

        if (progressDisposer.isCallProgress()) {
            progressDisposer.onProgress(downloadExecutor, sTotalLength, sSofarLength);
        }
        if (speedDisposer.isCallSpeed()) {
            speedDisposer.onSpeed(downloadExecutor, sSpeedLength);
            sSpeedLength = 0;
        }
    }

    private void onComplete() {
        //完成回调
        final OnDownloadListener onDownloadListener = httpRequest.downloadListener();
        if (onDownloadListener != null) {
            Schedulers schedulers = httpRequest.schedulers();
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        onDownloadListener.onComplete(downloadExecutor);
                    }
                });
            } else {
                onDownloadListener.onComplete(downloadExecutor);
            }
        }
    }

    public void cancel(){
        cancelAtomic.getAndSet(true);
    }

    private void checkIsCancel() {
        if (cancelAtomic.get()) {
            throw new CancelTaskException();
        }
    }
}
