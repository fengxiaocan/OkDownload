package com.x.down.task;


import com.x.down.ExecutorGather;
import com.x.down.XDownload;
import com.x.down.base.IConnectRequest;
import com.x.down.base.IDownloadRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.impl.DownloadListenerDisposer;
import com.x.down.made.AutoRetryRecorder;
import com.x.down.made.M3u8DownloaderBlock;
import com.x.down.made.M3u8DownloaderInfo;
import com.x.down.tool.XDownUtils;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

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

    public final void setTaskFuture(Future taskFuture) {
        this.taskFuture = taskFuture;
    }

    @Override
    protected void onExecute() throws Throwable {
        //获取之前的下载信息
        M3u8DownloaderInfo info = InfoSerializeProxy.readM3u8DownloadInfo(httpRequest);

        //判断一下文件的长度是否获取得到
        if (info == null || info.getBlockList() == null) {
            if (info == null) {
                info = new M3u8DownloaderInfo();
                info.setOriginalUrl(httpRequest.getConnectUrl());
            }
            //m3u8解析地址
            List<String> list = new ArrayList<>();

            if (!redirectResponse(info, list))
                return;

            if (list.isEmpty()) {
                //失败重试
                retryToRun();
                return;
            }

            //获取所有的 ts文件的地址
            ArrayList<M3u8DownloaderBlock> m3U8DownloaderBlocks = new ArrayList<>();
            URL url;
            if (info.getRedirectUrl() != null) {
                url = new URL(info.getRedirectUrl());
            } else {
                url = new URL(info.getOriginalUrl());
            }

            for (String name : list) {
                M3u8DownloaderBlock block = new M3u8DownloaderBlock();
                block.setUrl(getRedirectsUrl(url, name));
                block.setName(XDownUtils.getUrlName(block.getUrl()));
                m3U8DownloaderBlocks.add(block);
            }

            info.setBlockList(m3U8DownloaderBlocks);
            //保存下来
            InfoSerializeProxy.writeM3u8DownloadInfo(httpRequest, info);
        }

        //判断下载方式
        if (httpRequest.isUseMultiThread() && httpRequest.getDownloadMultiThreadSize() > 1) {
            //多线程下载
            multiThreadRun(info.getBlockList());
        } else {
            //单线程下载
            SingleDownloadM3u8Task threadTask = new SingleDownloadM3u8Task(httpRequest,
                    listenerDisposer,
                    info.getBlockList());
            XDownload.get().removeDownload(httpRequest.getTag());
            if (!threadTask.checkComplete()) {
                Future<?> future = ExecutorGather.executorDownloaderQueue().submit(threadTask);
                threadTask.setTaskFuture(future);
                XDownload.get().addDownload(httpRequest.getTag(), threadTask);
            }
        }
    }

    /**
     * 解析重定向的数据
     *
     * @param info
     * @param list
     * @return
     * @throws Exception
     */
    private boolean redirectResponse(M3u8DownloaderInfo info, List<String> list) throws Exception {
        String redirectResponse = info.getRedirectResponse();
        if (redirectResponse != null) {
            splitStringList(list, redirectResponse);
            return true;
        } else {
            if (info.getRedirectUrl() != null) {
                return getResponse(info.getRedirectUrl(), info, list, true);
            } else {
                return originalResponse(info, list);
            }
        }
    }

    /**
     * 解析原始链接的数据
     *
     * @param info
     * @param list
     * @return
     * @throws Exception
     */
    private boolean originalResponse(M3u8DownloaderInfo info, List<String> list) throws Exception {
        String originalResponse = info.getOriginalResponse();
        if (originalResponse != null) {
            //获取原始的下载信息,分割字符串
            splitStringList(list, originalResponse);
        }
        //如果为空
        if (list.isEmpty()) {
            //重新下载原始的下载信息
            boolean response = getResponse(info.getOriginalUrl(), info, list, false);
            if (response) {
                //判断是否需要重定向
                if (info.getRedirectUrl() != null) {
                    return redirectResponse(info, list);
                }
            }
            return response;
        }
        return true;//成功!
    }

    /**
     * 分割地址
     *
     * @param list
     * @param redirectResponse
     * @return
     */
    private void splitStringList(List<String> list, String redirectResponse) {
        list.clear();
        String[] split = redirectResponse.split("\r?\n");
        for (String key : split) {
            if (!key.startsWith("#")) {
                list.add(key);
            }
        }
    }

    /**
     * 获取指定地址的响应结果
     *
     * @param url
     * @return false 需要重试
     * @throws Exception
     */
    private boolean getResponse(String url, M3u8DownloaderInfo info, List<String> list, boolean isRedirect) throws Exception {
        HttpURLConnection http = httpRequest.buildConnect(url);
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
            retryToRun();
            return false;//不成功,需要重试
        }
        String response = readStringStream(http.getInputStream(), XDownUtils.getInputCharset(http));
        //重新分割一下字符串
        splitStringList(list, response);
        //原始的下载地址
        if (list.size() == 1) {
            String redirectUrl = list.get(0);
            if (redirectUrl.endsWith(".m3u8")) {
                //设置重定向的地址
                info.setRedirectUrl(getRedirectsUrl(http.getURL(), redirectUrl));
            }
        }
        if (isRedirect) {
            info.setRedirectResponse(response);
        } else {
            info.setOriginalResponse(response);
        }
        //断开连接
        XDownUtils.disconnectHttp(http);
        return true;//成功
    }


    private void multiThreadRun(List<M3u8DownloaderBlock> list) {
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

        threadPoolExecutor = ExecutorGather.newSubTaskQueue(httpRequest.getDownloadMultiThreadSize());

        final CountDownLatch countDownLatch = new CountDownLatch(list.size());//计数器
        final MultiM3u8Disposer disposer = new MultiM3u8Disposer(httpRequest, countDownLatch, getFilePath(), list, listenerDisposer);

        String filePath = getFilePath();
        for (int i = 0; i < list.size(); i++) {
            M3u8DownloaderBlock m3U8DownloaderBlock = list.get(i);
            //保存的临时文件
            File tempM3u8 = new File(tempCacheDir, m3U8DownloaderBlock.getName());

            MultiDownloadM3u8Task task = new MultiDownloadM3u8Task(httpRequest,
                    filePath,
                    m3U8DownloaderBlock,
                    tempM3u8,
                    autoRetryRecorder,
                    i,
                    disposer);
            Future<?> submit = threadPoolExecutor.submit(task);
            XDownload.get().addDownload(httpRequest.getTag(), task);
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

    @Override
    protected void onRetry() {
        listenerDisposer.onRetry(this);
    }

    @Override
    protected void onError(Throwable e) {
        listenerDisposer.onFailure(this);
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
        File saveFile = httpRequest.getSaveFile();
        if (saveFile != null) {
            return saveFile;
        }
        return new File(httpRequest.getSaveDir(), httpRequest.getSaveName() + ".mp4");
    }

    @Override
    public String getFilePath() {
        return getFile().getAbsolutePath();
    }

    @Override
    public long getTotalLength() {
        return 0;
    }

    @Override
    public long getSofarLength() {
        return 0;
    }
}
