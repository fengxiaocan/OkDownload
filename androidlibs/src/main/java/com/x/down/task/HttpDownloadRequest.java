package com.x.down.task;

import com.x.down.core.XDownloadRequest;
import com.x.down.made.AutoRetryRecorder;
import com.x.down.made.DownInfo;
import com.x.down.proxy.SerializeProxy;
import com.x.down.tool.XDownUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

abstract class HttpDownloadRequest extends BaseHttpRequest {

    protected final int byteArraySize;

    public HttpDownloadRequest(AutoRetryRecorder autoRetryRecorder, int byteSize) {
        super(autoRetryRecorder);
        byteArraySize = Math.max(2048, byteSize);
    }

    //进度回调
    protected void onProgress(int length) {
    }

    //连接中
    protected void onConnecting(HttpURLConnection http, long length) {
    }

    //获取文件长度
    protected HttpURLConnection getDownloaderLong(XDownloadRequest request) throws Exception {
        HttpURLConnection http = request.buildConnect();
        int responseCode = http.getResponseCode();

        while (isNeedRedirects(responseCode)) {
            http = redirectsConnect(http, request);
            responseCode = http.getResponseCode();
        }
        //优先获取文件长度再回调
        long contentLength = XDownUtils.getContentLength(http);
        onConnecting(http, contentLength);
        if (contentLength <= 0) {
            //长度获取不到的时候重新连接 获取不到长度则要求http请求不要gzip压缩
            XDownUtils.disconnectHttp(http);
            http = request.buildConnect();
            http.setRequestProperty("Accept-Encoding", "identity");
            http.connect();

            contentLength = XDownUtils.getContentLength(http);
            onConnecting(http, contentLength);
        }
        //是否支持断点下载
        boolean acceptRanges = isAcceptRanges(http);
        //保存下载信息
        SerializeProxy.writeDownloaderInfo(request, new DownInfo(contentLength, acceptRanges));
        return http;
    }

    protected final boolean readInputStream(InputStream is, OutputStream os) throws IOException {
        try {
            byte[] bytes = new byte[byteArraySize];
            int length;
            while ((length = is.read(bytes)) > 0) {
                if (isCancel) {
                    onCancel();
                    return false;
                }
                os.write(bytes, 0, length);
                os.flush();
                onProgress(length);
            }
            return true;
        } finally {
            XDownUtils.closeIo(is);
            XDownUtils.closeIo(os);
        }
    }

    /**
     * 是否支持断点下载
     *
     * @param http
     * @return
     */
    protected final boolean isAcceptRanges(HttpURLConnection http) {
        String field = http.getHeaderField("Accept-Ranges");
        if (!XDownUtils.isEmpty(field)) {
            return !field.contains("none");
        }
        return true;
    }
}
