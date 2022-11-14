package com.x.down.task;


import com.x.down.ExecutorGather;
import com.x.down.XDownload;
import com.x.down.base.IConnectRequest;
import com.x.down.base.IDownloadRequest;
import com.x.down.core.XDownloadRequest;
import com.x.down.impl.DownloadListenerDisposer;
import com.x.down.made.AutoRetryRecorder;
import com.x.down.made.DownloaderBlock;
import com.x.down.made.DownloaderInfo;
import com.x.down.tool.XDownUtils;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 下载之前需要获取请求信息
 */
final class DownloadThreadRequest extends HttpDownloadRequest implements IDownloadRequest, IConnectRequest {

    protected final XDownloadRequest httpRequest;
    protected final DownloadListenerDisposer listenerDisposer;
    protected ThreadPoolExecutor threadPoolExecutor;
    protected volatile Future taskFuture;
    protected volatile long sContentLength;

    public DownloadThreadRequest(
            XDownloadRequest request) {
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
    protected void onConnecting(HttpURLConnection http, long length) {
        sContentLength = length;
        listenerDisposer.onConnecting(this, getHeaders(http));
    }

    @Override
    protected void onExecute() throws Throwable {
        //是否支持断点下载
        boolean acceptRanges = true;
        //获取之前的下载信息
        if (sContentLength <= 0) {
            DownloaderInfo info = InfoSerializeProxy.readDownloaderInfo(httpRequest);
            if (info != null) {
                sContentLength = info.getContentLength();
                acceptRanges = info.isAccecp();
            }
        }

        //判断一下文件的长度是否获取得到
        if (sContentLength <= 0) {
            HttpURLConnection http = getDownloaderLong(httpRequest);
            //是否支持断点下载
            acceptRanges = isAcceptRanges(http);
            //获取文件类型后缀
            final int code = http.getResponseCode();
            if (!isSuccess(code)) {
                //获取错误信息
                String stream = readStringStream(http.getErrorStream(), XDownUtils.getInputCharset(http));
                listenerDisposer.onRequestError(this, code, stream);
                //断开请求
                XDownUtils.disconnectHttp(http);
                //重试
                retryToRun();
                return;
            } else {
                //先断开请求
                XDownUtils.disconnectHttp(http);
            }
        }

        //判断之前有没有下载完成文件
        if (sContentLength > 0) {
            File file = getFile();
            if (file.exists()) {
                if (file.length() == sContentLength) {
                    listenerDisposer.onComplete(this);
                    return;
                } else {
                    file.delete();
                }
            }
        }

        //判断执行多线程下载还是单线程下载
        if (acceptRanges && sContentLength > 0 && httpRequest.isUseMultiThread()) {
            multiThreadRun(sContentLength);
        } else {
            //独立下载
            SingleDownloadThreadTask threadTask = new SingleDownloadThreadTask(httpRequest,
                    listenerDisposer,
                    sContentLength);
            XDownload.get().removeDownload(httpRequest.getTag());
            if (!threadTask.checkComplete()) {
                Future<?> future = ExecutorGather.executorDownloaderQueue().submit(threadTask);
                threadTask.setTaskFuture(future);
                XDownload.get().addDownload(httpRequest.getTag(), threadTask);
            }
        }
    }

    @Override
    public void run() {
        super.run();
        taskFuture = null;
        XDownload.get().removeDownload(tag());
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

    /**
     * 多线程下载
     *
     * @param totalLength
     */
    private void multiThreadRun(final long totalLength) {
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
        File cacheDir = XDownUtils.getTempCacheDir(httpRequest,true);

        //是否需要删除之前的临时文件
        final boolean isDelectTemp = !httpRequest.isUseBreakpointResume();
        if (isDelectTemp) {
            //需要删除之前的临时缓存文件
            XDownUtils.deleteDir(cacheDir);
        }

        DownloaderBlock block = InfoSerializeProxy.readDownloaderBlock(httpRequest);

        if (block == null) {
            block = createBlock(totalLength);
            InfoSerializeProxy.writeDownloaderBlock(httpRequest, block);
        }

        //每一块的长度
        final long blockLength = block.getBlockLength();
        //需要的执行任务数量
        final int threadCount = block.getThreadCount();

        threadPoolExecutor = ExecutorGather.newSubTaskQueue(httpRequest.getDownloadMultiThreadSize());

        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);//计数器
        final MultiDownloadDisposer disposer = new MultiDownloadDisposer(httpRequest, countDownLatch, threadCount, listenerDisposer, totalLength);

        synchronized (Object.class) {
            long start = 0, end = -1;
            final String filePath = getFilePath();

            for (int index = 0; index < threadCount; index++) {
                start = end + 1;
                final long fileLength;
                if (index == threadCount - 1) {
                    end = totalLength;
                    fileLength = totalLength - start;
                } else {
                    end = start + blockLength;
                    fileLength = blockLength + 1;
                }
                //保存的临时文件
                File file = new File(cacheDir, httpRequest.getIdentifier() + "_temp_" + index);
                //任务
                MultiDownloadThreadTask task = new MultiDownloadThreadTask(httpRequest, file, filePath,
                        autoRetryRecorder, fileLength, start, end, disposer);

                disposer.addTask(task);
                Future<?> submit = threadPoolExecutor.submit(task);
                XDownload.get().addDownload(httpRequest.getTag(), task);
                task.setTaskFuture(submit);
            }
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
     * 创建块
     *
     * @param contentLength
     * @return
     */
    protected DownloaderBlock createBlock(final long contentLength) {
        final long blockLength;
        //使用的线程数
        final int threadCount;

        final int configThreadCount = httpRequest.getDownloadMultiThreadSize();
        final int threadMaxSize = httpRequest.getMaxDownloadBlockSize();
        final int threadMinSize = httpRequest.getMinDownloadBlockSize();
        //最大的数量
        final long maxLength = configThreadCount * threadMaxSize;
        final long minLength = configThreadCount * threadMinSize;
        //智能计算执行任务的数量
        if (contentLength <= minLength) {
            //如果文件过小,设定的线程有浪费,控制线程的创建少于设定的线程
            threadCount = 1;
            blockLength = minLength;
        } else if (contentLength > maxLength) {
            //如果文件过大,设定的线程不足够
            blockLength = threadMaxSize;
            threadCount = (int) (contentLength / blockLength);
        } else {
            //正常的线程
            blockLength = contentLength / configThreadCount;
            threadCount = configThreadCount;
        }
        return new DownloaderBlock(contentLength, blockLength, threadCount);
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

    protected File getFile() {
        return httpRequest.getSaveFile();
    }

    @Override
    public String getFilePath() {
        return getFile().getAbsolutePath();
    }
}
