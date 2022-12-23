package com.x.down.task;


import com.x.down.XDownload;
import com.x.down.base.IConnectRequest;
import com.x.down.base.IDownloadRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.impl.DownloadListenerDisposer;
import com.x.down.listener.OnM3u8ParseIntercept;
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
    protected void completeRun() {
        XDownload.get().removeDownload(tag());
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
            info = parseM3U8Info();
            if (info != null && info.isNeedRedirect()) {
                info = getM3u8Response(info.getUrl(), false);
            }
        }
        checkIsCancel();

        if (info == null) {
            info = SerializeProxy.readM3u8Info(httpRequest);
            if (info != null && info.isNeedRedirect()) {
                info = getM3u8Response(info.getUrl(), false);
            }
        }

        checkIsCancel();

        //判断一下文件的长度是否获取得到
        if (info == null || info.getTsList().size() == 0) {
            info = getM3u8Response(httpRequest.getConnectUrl(), true);
            if (info == null || info.getTsList().size() == 0) {
                return;
            }
        }

        checkIsCancel();

        //判断下载方式
        if (httpRequest.isUseMultiThread() && httpRequest.getDownloadMultiThreadSize() > 1) {
            //多线程下载
            multiThreadRun(info);
        } else {
            //单线程下载
            SingleDownloadM3u8Task threadTask = new SingleDownloadM3u8Task(httpRequest,
                    listenerDisposer, info);
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
    private M3U8Info getM3u8Response(String baseUrl, boolean retry) throws Exception {
        HttpURLConnection http = httpRequest.buildConnect(baseUrl);

        checkIsCancel();

        int responseCode = http.getResponseCode();
        //判断是否需要重定向
        while (isNeedRedirects(responseCode)) {
            http = redirectsConnect(http, httpRequest);
            responseCode = http.getResponseCode();
        }

        checkIsCancel();

        if (!isSuccess(responseCode)) {
            //获取错误信息
            String stream = readStringStream(http.getErrorStream(), XDownUtils.getInputCharset(http));
            listenerDisposer.onRequestError(this, responseCode, stream);
            //断开请求
            XDownUtils.disconnectHttp(http);

            if (retry) {
                //不成功,需要重试
                tryToRetry(responseCode, stream);
            }
            return null;
        }

        M3U8Info info = parseNetworkM3U8Info(baseUrl, http.getInputStream());
        XDownUtils.disconnectHttp(http);

        checkIsCancel();

        if (info.isNeedRedirect() && !info.getUrl().equals(baseUrl)) {
            //防止无限循环
            return getM3u8Response(info.getUrl(), retry);
        }
        //保存下来
        SerializeProxy.writeM3u8Info(httpRequest, info);
        M3U8Utils.createNetM3U8(getM3u8NetFile(),  info);

        return info;
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
            checkIsCancel();

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
        threadPoolExecutor.shutdownNow();
    }

    /**
     * 检测是否已经下载完成
     *
     * @return
     */
    public boolean checkComplete() {
        File save = getFile();
        if (save.exists() && save.length()>0){
            try {
                M3U8Utils.parseLocalM3u8File(save);
                return true;
            } catch (Exception e) {

            }
        }
        M3U8Info info = SerializeProxy.readM3u8Info(httpRequest);

        if (info == null) {
            File saveFile = getM3u8NetFile();
            boolean b = saveFile.exists() && saveFile.length() > 0;
            if (!b) return false;
            try {
                info = M3U8Utils.parseLocalM3u8File(saveFile);
            } catch (Exception e) {
                return false;
            }
        }

        File m3u8Dir = httpRequest.getM3u8Dir();
        for (M3U8Ts ts : info.getTsList()) {
            if (ts.hasInitSegment()) {
                File file = ts.getInitSegmentFile(m3u8Dir);
                if (!file.exists()) {
                    return false;
                }
                if (ts.getInitSegmentLength() <= 0) {
                    return false;
                }
                if (file.length() < ts.getInitSegmentLength()) {
                    return false;
                }
            }
            if (ts.hasKey()) {
                File file = ts.getInitSegmentFile(m3u8Dir);
                if (!file.exists() || file.length() == 0) {
                    return false;
                }
            }
            File file = ts.getTsFile(m3u8Dir);
            if (!file.exists()) {
                return false;
            }
            if (ts.getTsSize() <= 0) {
                return false;
            }
            if (file.length() < ts.getTsSize()) {
                return false;
            }
        }
        return true;
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
        cancelTask();
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdownNow();
        }
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

    /**
     * 保存m3u8网络信息的文件
     *
     * @return
     */
    private File getM3u8NetFile() {
        //保存m3u8信息
        return new File(getFile().getAbsolutePath() + "_net.m3u8");
    }

    private File getFile() {
        return httpRequest.getSaveFile();
    }

    @Override
    public String getFilePath() {
        return getFile().getAbsolutePath();
    }

    private M3U8Info parseM3U8Info() throws Exception {
        String baseUrl = httpRequest.getConnectUrl();
        if (httpRequest.getM3u8Info() != null) {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new StringReader(httpRequest.getM3u8Info()));
                return parseNetworkM3U8Info(baseUrl, bufferedReader);
            } finally {
                XDownUtils.closeIo(bufferedReader);
            }
        }
        if (httpRequest.getM3u8Path() != null) {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(httpRequest.getM3u8Path())));
                return parseNetworkM3U8Info(baseUrl, bufferedReader);
            } finally {
                XDownUtils.closeIo(bufferedReader);
            }
        }
        return null;
    }

    /**
     * 解析网络的m3u8文件
     *
     * @param baseUrl
     * @return
     * @throws Exception
     */
    private M3U8Info parseNetworkM3U8Info(String baseUrl, InputStream inputStream) throws Exception {
        OnM3u8ParseIntercept intercept = httpRequest.getOnM3u8ParseIntercept();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            if (intercept != null) {
                M3U8Info info = intercept.intercept(bufferedReader);
                if (info != null && !info.isNeedRedirect() && info.getTsList().size() > 0) {
                    return info;
                }
            }
            return M3U8Utils.parseNetworkM3U8Info(baseUrl, bufferedReader);
        } finally {
            XDownUtils.closeIo(bufferedReader);
        }
    }

    /**
     * 解析已经下载在本地的m3u8文件
     *
     * @param baseUrl
     * @param reader
     * @return
     * @throws Exception
     */
    private M3U8Info parseNetworkM3U8Info(String baseUrl, BufferedReader reader) throws Exception {
        OnM3u8ParseIntercept intercept = httpRequest.getOnM3u8ParseIntercept();
        try {
            if (intercept != null) {
                M3U8Info info = intercept.intercept(reader);
                if (info != null && !info.isNeedRedirect() && info.getTsList().size() > 0) {
                    return info;
                }
            }
            return M3U8Utils.parseNetworkM3U8Info(baseUrl, reader);
        } finally {
            XDownUtils.closeIo(reader);
        }
    }


}
