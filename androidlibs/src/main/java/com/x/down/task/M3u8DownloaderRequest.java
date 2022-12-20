package com.x.down.task;


import com.x.down.XDownload;
import com.x.down.base.IConnectRequest;
import com.x.down.base.IDownloadRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.impl.DownloadListenerDisposer;
import com.x.down.m3u8.M3U8Constants;
import com.x.down.m3u8.M3U8Info;
import com.x.down.m3u8.M3U8Ts;
import com.x.down.m3u8.M3U8Utils;
import com.x.down.made.AutoRetryRecorder;
import com.x.down.proxy.SerializeProxy;
import com.x.down.tool.XDownUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

final class M3u8DownloaderRequest extends HttpDownloadRequest implements IDownloadRequest, IConnectRequest {

    protected final XDownloadRequest httpRequest;
    protected final DownloadListenerDisposer listenerDisposer;
    protected ThreadPoolExecutor threadPoolExecutor;
    protected volatile Future taskFuture;

    public M3u8DownloaderRequest(XDownloadRequest request) {
        super(new AutoRetryRecorder(request.isUseAutoRetry(),
                request.getAutoRetryTimes(),
                request.getAutoRetryInterval()), request.getBufferedSize());
        this.httpRequest = request;
        this.listenerDisposer = new DownloadListenerDisposer(request);
    }

    public void setTaskFuture(Future taskFuture) {
        this.taskFuture = taskFuture;
    }

    @Override
    public void run() {
        super.run();
        taskFuture = null;
    }

    @Override
    protected void onExecute() throws Throwable {
        if (checkComplete()) {
            listenerDisposer.onComplete(this);
            return;
        }
        //获取之前的下载信息
        M3U8Info info = null;
        if (httpRequest.isAsM3u8()) {
            if (httpRequest.getM3u8Info() != null) {
                info = new M3U8Info();
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(new StringReader(httpRequest.getM3u8Info()));
                    if (!parseNetworkM3U8Info(info, bufferedReader)) {
                        info = null;
                    }
                } finally {
                    XDownUtils.closeIo(bufferedReader);
                }
            }
            if (info == null && httpRequest.getM3u8Path() != null) {
                info = new M3U8Info();
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(httpRequest.getM3u8Path())));
                    if (!parseNetworkM3U8Info(info, bufferedReader)) {
                        info = null;
                    }
                } finally {
                    XDownUtils.closeIo(bufferedReader);
                }
            }
        }
        if (info == null) {
            info = SerializeProxy.readM3u8Info(httpRequest);
        }

        //判断一下文件的长度是否获取得到
        if (info == null || info.getTsList() == null) {
            if (info == null) {
                info = new M3U8Info();
            }
            info.setUrl(httpRequest.getConnectUrl());

            if (!getM3u8Response(info)) {
                return;
            }

            //保存下来
            SerializeProxy.writeM3u8Info(httpRequest, info);
            //保存m3u8信息
            File m3u8File = new File(getFile().getAbsolutePath().replace(".m3u8", "_net.m3u8"));
            File tempCacheDir = XDownUtils.getTempCacheDir2(request());
            M3U8Utils.createNetM3U8(m3u8File, tempCacheDir, info);
        }


        //判断下载方式
        if (httpRequest.isUseMultiThread() && httpRequest.getDownloadMultiThreadSize() > 1) {
            //多线程下载
            multiThreadRun(info);
        } else {
            //单线程下载
            SingleDownloadM3u8Task threadTask = new SingleDownloadM3u8Task(httpRequest,
                    listenerDisposer, info);
            XDownload.get().removeDownload(httpRequest.getTag());
            Future<?> future = XDownload.executorDownloaderQueue().submit(threadTask);
            threadTask.setTaskFuture(future);
            XDownload.get().addDownload(httpRequest.getTag(), threadTask);
        }
    }


    /**
     * 获取指定地址的响应结果
     *
     * @return false 需要重试
     * @throws Exception
     */
    private boolean getM3u8Response(M3U8Info info) throws Exception {
        HttpURLConnection http = httpRequest.buildConnect(info.getUrl());
        int responseCode = http.getResponseCode();
        //判断是否需要重定向
        while (isNeedRedirects(responseCode)) {
            http = redirectsConnect(http, httpRequest);
            responseCode = http.getResponseCode();
        }

        if (!isSuccess(responseCode)) {
            //获取错误信息
            String stream = readStringStream(http.getErrorStream(), XDownUtils.getInputCharset(http));
            listenerDisposer.onRequestError(this, responseCode, stream);
            //断开请求
            XDownUtils.disconnectHttp(http);
            //重试
            retryToRun(responseCode, stream);
            return false;//不成功,需要重试
        }

        boolean isSuccess = parseNetworkM3U8Info(info, http.getInputStream());
        XDownUtils.disconnectHttp(http);
        if (isSuccess) {
            return true;
        } else {
            return getM3u8Response(info);
        }
    }


    private void multiThreadRun(M3U8Info info) {
        if (threadPoolExecutor != null) {
            //isShutDown：当调用shutdown()或shutdownNow()方法后返回为true。 
            //isTerminated：当调用shutdown()方法后，并且所有提交的任务完成后返回为true;
            //isTerminated：当调用shutdownNow()方法后，成功停止后返回为true;
            if (threadPoolExecutor.isShutdown()) {
                //线程已经开始
                return;
            }
            if (!threadPoolExecutor.isTerminated()) {
                //线程已开始并且还没完成
                return;
            }
        }
        //获取上次配置,决定断点下载不出错
        File tempCacheDir = XDownUtils.getTempCacheDir(httpRequest);

        //是否需要删除之前的临时文件
        final boolean isDelectTemp = !httpRequest.isUseBreakpointResume();
        if (isDelectTemp) {
            //需要删除之前的临时缓存文件
            XDownUtils.deleteDir(tempCacheDir);
        }
        tempCacheDir.mkdirs();

        threadPoolExecutor = XDownload.newSubTaskQueue(httpRequest.getDownloadMultiThreadSize());

        final CountDownLatch countDownLatch = new CountDownLatch(info.getTsList().size());//计数器
        final MultiM3u8Disposer disposer = new MultiM3u8Disposer(httpRequest, countDownLatch, getFile(), info, listenerDisposer);

        String filePath = getFilePath();
        final ReentrantLock lock = new ReentrantLock();
        for (int i = 0; i < info.getTsList().size(); i++) {
            MultiDownloadM3u8Task task = new MultiDownloadM3u8Task(httpRequest,
                    filePath,
                    info,
                    tempCacheDir,
                    autoRetryRecorder,
                    i,
                    disposer,
                    lock);
            Future<?> submit = threadPoolExecutor.submit(task);
            XDownload.get().addDownload(httpRequest.getTag(), task);
            disposer.addTask(task);
            task.setTaskFuture(submit);
        }

        //等待下载完成
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        XDownload.get().removeDownload(httpRequest.getTag());
        threadPoolExecutor.shutdown();
    }

    /**
     * 检测是否已经下载完成
     *
     * @return
     */
    public boolean checkComplete() {
        File saveFile = getFile();
        boolean b = saveFile.exists() && saveFile.length() > 0;
        if (b) {
            try {
                M3U8Info info = M3U8Utils.parseLocalM3U8File(saveFile);
                for (M3U8Ts ts : info.getTsList()) {
                    if (ts.hasInitSegment()) {
                        File file = new File(ts.getInitSegmentName());
                        if (!file.exists() || file.length() <= 0) {
                            return false;
                        }
                    }
                    File file = new File(ts.getIndexName());
                    if (!file.exists() || file.length() <= 0) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
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
    public String tag() {
        return httpRequest.getTag();
    }

    @Override
    public String url() {
        return httpRequest.getConnectUrl();
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
        return httpRequest;
    }

    private File getFile() {
        return httpRequest.getSaveFile();
    }

    @Override
    public String getFilePath() {
        return getFile().getAbsolutePath();
    }

    private boolean parseNetworkM3U8Info(M3U8Info m3U8Info, InputStream inputStream) throws Exception {
        return parseNetworkM3U8Info(m3U8Info, new BufferedReader(new InputStreamReader(inputStream)));
    }

    private boolean parseNetworkM3U8Info(M3U8Info m3U8Info, BufferedReader bufferedReader) throws Exception {
        String videoUrl = m3U8Info.getUrl();
        float tsDuration = 0;
        int targetDuration = 0;
        int tsIndex = 0;
        int version = 0;
        int sequence = 0;
        boolean hasDiscontinuity = false;
        boolean hasEndList = false;
        boolean hasStreamInfo = false;
        boolean hasKey = false;
        boolean hasInitSegment = false;
        String method = null;
        String encryptionIV = null;
        String encryptionKeyUri = null;
        String initSegmentUri = null;
        String segmentByteRange = null;
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            if (XDownUtils.isEmpty(line)) {
                continue;
            }
            if (line.startsWith(M3U8Constants.TAG_PREFIX)) {
                if (line.startsWith(M3U8Constants.TAG_MEDIA_DURATION)) {
                    String ret = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_MEDIA_DURATION);
                    if (!XDownUtils.isEmpty(ret)) {
                        tsDuration = Float.parseFloat(ret);
                    }
                } else if (line.startsWith(M3U8Constants.TAG_TARGET_DURATION)) {
                    String ret = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_TARGET_DURATION);
                    if (!XDownUtils.isEmpty(ret)) {
                        targetDuration = Integer.parseInt(ret);
                    }
                } else if (line.startsWith(M3U8Constants.TAG_VERSION)) {
                    String ret = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_VERSION);
                    if (!XDownUtils.isEmpty(ret)) {
                        version = Integer.parseInt(ret);
                    }
                } else if (line.startsWith(M3U8Constants.TAG_MEDIA_SEQUENCE)) {
                    String ret = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_MEDIA_SEQUENCE);
                    if (!XDownUtils.isEmpty(ret)) {
                        sequence = Integer.parseInt(ret);
                    }
                } else if (line.startsWith(M3U8Constants.TAG_STREAM_INF)) {
                    hasStreamInfo = true;
                } else if (line.startsWith(M3U8Constants.TAG_DISCONTINUITY)) {
                    hasDiscontinuity = true;
                } else if (line.startsWith(M3U8Constants.TAG_ENDLIST)) {
                    hasEndList = true;
                } else if (line.startsWith(M3U8Constants.TAG_KEY)) {
                    hasKey = true;
                    method = M3U8Utils.parseOptionalStringAttr(line, M3U8Constants.REGEX_METHOD);
                    String keyFormat = M3U8Utils.parseOptionalStringAttr(line, M3U8Constants.REGEX_KEYFORMAT);
                    if (!M3U8Constants.METHOD_NONE.equals(method)) {
                        encryptionIV = M3U8Utils.parseOptionalStringAttr(line, M3U8Constants.REGEX_IV);
                        if (M3U8Constants.KEYFORMAT_IDENTITY.equals(keyFormat) || keyFormat == null) {
                            if (M3U8Constants.METHOD_AES_128.equals(method)) {
                                // The segment is fully encrypted using an identity key.
                                String tempKeyUri = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_URI);
                                if (tempKeyUri != null) {
                                    encryptionKeyUri = M3U8Utils.getM3U8AbsoluteUrl(videoUrl, tempKeyUri);
                                }
                            } else {
                                // Do nothing. Samples are encrypted using an identity key,
                                // but this is not supported. Hopefully, a traditional DRM
                                // alternative is also provided.
                            }
                        } else {
                            // Do nothing.
                        }
                    }
                } else if (line.startsWith(M3U8Constants.TAG_INIT_SEGMENT)) {
                    String tempInitSegmentUri = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_URI);
                    if (!XDownUtils.isEmpty(tempInitSegmentUri)) {
                        hasInitSegment = true;
                        initSegmentUri = M3U8Utils.getM3U8AbsoluteUrl(videoUrl, tempInitSegmentUri);
                        segmentByteRange = M3U8Utils.parseOptionalStringAttr(line, M3U8Constants.REGEX_ATTR_BYTERANGE);
                    }
                }
                continue;
            }
            // It has '#EXT-X-STREAM-INF' tag;
            if (hasStreamInfo) {
                m3U8Info.setUrl(M3U8Utils.getM3U8AbsoluteUrl(videoUrl, line));
                return false;
            }
            if (Math.abs(tsDuration) < 0.001f) {
                continue;
            }
            M3U8Ts ts = new M3U8Ts();
            ts.initTsAttributes(M3U8Utils.getM3U8AbsoluteUrl(videoUrl, line), tsDuration, tsIndex, sequence++, hasDiscontinuity);
            if (hasKey) {
                ts.setKeyConfig(method, encryptionKeyUri, encryptionIV);
            }
            if (hasInitSegment) {
                ts.setInitSegmentInfo(initSegmentUri, segmentByteRange);
            }
            m3U8Info.addTs(ts);
            tsIndex++;
            tsDuration = 0;
            hasStreamInfo = false;
            hasDiscontinuity = false;
            hasKey = false;
            hasInitSegment = false;
            method = null;
            encryptionKeyUri = null;
            encryptionIV = null;
            initSegmentUri = null;
            segmentByteRange = null;
        }
        m3U8Info.setTargetDuration(targetDuration);
        m3U8Info.setVersion(version);
        m3U8Info.setSequence(sequence);
        m3U8Info.setHasEndList(hasEndList);
        return true;
    }
}
