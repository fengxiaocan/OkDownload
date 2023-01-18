package com.ok.request.down;

import com.ok.request.CoreDownload;
import com.ok.request.base.DownloadExecutor;
import com.ok.request.call.RequestCall;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.exception.CancelTaskException;
import com.ok.request.exception.HttpErrorException;
import com.ok.request.executor.AutoRetryExecutor;
import com.ok.request.factory.SerializeFactory;
import com.ok.request.info.DownInfo;
import com.ok.request.info.DownloaderBlock;
import com.ok.request.listener.OnDownloadListener;
import com.ok.request.request.HttpResponse;
import com.ok.request.request.Request;
import com.ok.request.request.Response;
import com.ok.request.tool.XDownUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

public class HttpDownloadExecutor extends AutoRetryExecutor implements DownloadExecutor {
    private final OkDownloadRequest httpRequest;
    private final RequestCall httpCall = new RequestCall();
    private final AtomicLong contentLength = new AtomicLong();
    private final File saveFile;
    private ThreadPoolExecutor threadPoolExecutor;
    private SingleDownloadConnection downloadConnection;

    public HttpDownloadExecutor(OkDownloadRequest request) {
        super(request);
        CoreDownload.addExecute(tag(), this);
        httpRequest = request;
        saveFile = httpRequest.getSaveFile();

        httpCall.setNetworkInterceptors(httpRequest.getNetworkInterceptor());
        httpCall.setInterceptors(httpRequest.getInterceptor());
    }

    @Override
    protected void onExecute() throws Throwable {
        //是否需要使用多线程
        final boolean usedMultiThread = httpRequest.isUsedMultiThread();
        //是否支持断点下载
        boolean acceptRanges = httpRequest.isUseBreakpointResume();
        //检测是否完成
        if (checkComplete(acceptRanges)) {
            return;
        }
        //获取之前的下载信息
        if (contentLength.get() <= 0) {
            DownInfo info = SerializeFactory.readDownloaderInfo(httpRequest);
            if (info != null && info.getLength() > 0) {
                contentLength.getAndSet(info.getLength());
                acceptRanges = acceptRanges && info.isAccecp();
                //检测是否完成
                if (checkComplete(acceptRanges)) {
                    return;
                }
            } else {
                //创建请求
                Request request = httpRequest.request();
                //http请求不要gzip压缩
                request.addHeader("Accept-Encoding", "identity");
                try {
                    Response response = httpCall.process(request);

                    if (response.isSuccess()) {
                        //成功
                        contentLength.getAndSet(response.body().contentLength());
                        //是否支持断点下载
                        acceptRanges = acceptRanges && XDownUtils.isAcceptRanges(response.headers());
                        //记录信息
                        SerializeFactory.writeDownloaderInfo(httpRequest, new DownInfo(contentLength.get(), acceptRanges));
                        //再次检测是否完成
                        if (checkComplete(acceptRanges)) {
                            return;
                        }

                        final int threadMinSize = httpRequest.getMinDownloadBlockSize();
                        final boolean singleThread = !usedMultiThread || contentLength.get() < threadMinSize;

                        if (singleThread) {
                            //如果使用单线程的,直接复用已经开启的连接
                            getSig().onExecute(httpCall, response, acceptRanges);
                            return;
                        }
                    } else {
                        //抛出错误
                        HttpResponse httpResponse = new HttpResponse(response);
                        String url = request.url().toString();
                        throw new HttpErrorException(response.code(), url, httpResponse.error());
                    }
                } finally {
                    httpCall.terminated();
                }
            }
        }
        final int threadMinSize = httpRequest.getMinDownloadBlockSize();
        final boolean singleThread = !usedMultiThread || contentLength.get() < threadMinSize;

        if (singleThread) {
            //如果使用单线程的,直接使用单线程连接
            getSig().onExecute(httpCall, null, contentLength.get(), acceptRanges);
        } else {
            multiThreadRun(acceptRanges);
        }
    }


    /**
     * 检测是否已经下载完成
     *
     * @return
     */
    private boolean checkComplete(boolean acceptRanges) {
        if (contentLength.get() > 0 && saveFile.exists()) {
            if (saveFile.length() == contentLength.get()) {
                onComplete();
                return true;
            } else if (!acceptRanges || contentLength.get() < saveFile.length()) {
                saveFile.deleteOnExit();
            }
        }
        return false;
    }

    private synchronized SingleDownloadConnection getSig() {
        if (downloadConnection == null) {
            downloadConnection = new SingleDownloadConnection(httpRequest, this);
        }
        return downloadConnection;
    }

    /**
     * 多线程下载
     */
    private void multiThreadRun(final boolean acceptRanges) {
        if (threadPoolExecutor != null) {
            if (threadPoolExecutor.isShutdown()) {
                return;
            }
            if (!threadPoolExecutor.isTerminated()) {
                return;
            }
        }
        //获取上次配置,决定断点下载不出错
        File cacheDir = XDownUtils.getTempCacheDir(httpRequest);

        //是否需要删除之前的临时文件
        if (!acceptRanges) {
            //需要删除之前的临时缓存文件
            XDownUtils.deleteDir(cacheDir);
        }
        cacheDir.mkdirs();

        DownloaderBlock block = SerializeFactory.readDownloaderBlock(httpRequest);

        final long totalLength = contentLength.get();
        if (block == null) {
            block = createBlock(totalLength);
            SerializeFactory.writeDownloaderBlock(httpRequest, block);
        }
        //每一块的长度
        final long blockLength = block.getBlockLength();
        //需要的执行任务数量
        final int threadCount = block.getThreadCount();

        threadPoolExecutor = CoreDownload.newSubTaskQueue(httpRequest.getDownloadMultiThreadSize());

        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);//计数器
        final MultiDownloadDisposer disposer = new MultiDownloadDisposer(httpRequest, countDownLatch, threadCount, totalLength);

        long start = 0, end = -1;
        final File filePath = saveFile();

        final List<Dispatcher> dispatcherList = new ArrayList<>();

        for (int index = 0; index < threadCount; index++) {
            checkIsCancel();

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
            MultiDownloadExecutor task = new MultiDownloadExecutor(httpRequest, file, filePath,
                     fileLength, start, end, disposer);
            dispatcherList.add(task);
            disposer.addTask(task);
            task.setTaskFuture(threadPoolExecutor.submit(task));
        }

        //等待下载完成
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            for (Dispatcher dispatcher : dispatcherList) {
                dispatcher.cancel();
            }
            throw new CancelTaskException();
        } finally {
            threadPoolExecutor.shutdownNow();
        }
    }

    /**
     * 创建块
     *
     * @param contentLength
     * @return
     */
    protected final DownloaderBlock createBlock(final long contentLength) {
        final long blockLength;
        //使用的线程数
        final int threadCount;

        final int configThreadCount = httpRequest.getDownloadMultiThreadSize();
        final int threadMaxSize = httpRequest.getMaxDownloadBlockSize();
        final int threadMinSize = httpRequest.getMinDownloadBlockSize();
        //最大的数量
        final long maxLength = ((long) configThreadCount) * threadMaxSize;
        final long minLength = ((long) configThreadCount) * threadMinSize;
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

    private void onComplete() {
        //完成回调
        final OnDownloadListener onDownloadListener = httpRequest.downloadListener();
        if (onDownloadListener != null) {
            Schedulers schedulers = httpRequest.schedulers();
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        onDownloadListener.onComplete(HttpDownloadExecutor.this);
                    }
                });
            } else {
                onDownloadListener.onComplete(this);
            }
        }
    }

    @Override
    protected void completeRun() {
        CoreDownload.removeExecute(tag(), this);
    }

    @Override
    protected void applyCancel() {
        httpCall.cancel();
        if (downloadConnection != null) {
            downloadConnection.cancel();
        }
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdownNow();
        }
    }

    @Override
    public File saveFile() {
        return httpRequest.getSaveFile();
    }

}
