package com.ok.request.m3u8;

import com.ok.request.CoreDownload;
import com.ok.request.base.DownloadExecutor;
import com.ok.request.call.RequestCall;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.exception.CancelTaskException;
import com.ok.request.exception.HttpErrorException;
import com.ok.request.exception.TimeoutException;
import com.ok.request.executor.AutoRetryExecutor;
import com.ok.request.factory.SerializeFactory;
import com.ok.request.info.M3U8Info;
import com.ok.request.listener.OnM3u8ParseIntercept;
import com.ok.request.request.HttpResponse;
import com.ok.request.request.Response;
import com.ok.request.tool.M3U8Utils;
import com.ok.request.tool.XDownUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLProtocolException;

public class M3u8DownloadExecutor extends AutoRetryExecutor implements DownloadExecutor {
    final AtomicReference<String> currentURL = new AtomicReference<>("");
    private final OkDownloadRequest httpRequest;
    private final RequestCall httpCall = new RequestCall();
    private final File saveFile;
    private ThreadPoolExecutor threadPoolExecutor;
    private SingleDownloadM3u8 singleDownloadM3u8;

    public M3u8DownloadExecutor(OkDownloadRequest request) {
        super(request);
        CoreDownload.addExecute(tag(), this);
        httpRequest = request;
        saveFile = httpRequest.getSaveFile();

        httpCall.setNetworkInterceptors(httpRequest.getNetworkInterceptor());
        httpCall.setInterceptors(httpRequest.getInterceptor());
    }

    @Override
    protected void onError(Throwable e) {
        if (e instanceof SSLProtocolException) {
            super.onError(new TimeoutException("Read timed out:" + currentURL.get(), e));
        } else if (e instanceof SocketTimeoutException) {
            super.onError(new TimeoutException("connect timed out:" + currentURL.get(), e));
        } else {
            super.onError(e);
        }
        httpRequest.callDownloadFailure(this);
    }

    @Override
    protected void onExecute() throws Throwable {
        //是否需要使用多线程
        final boolean usedMultiThread = httpRequest.isUsedMultiThread();
        //检测是否完成
        if (checkComplete()) {
            return;
        }
        //获取之前的下载信息
        M3U8Info info = SerializeFactory.readM3u8Info(httpRequest);
        info = m3u8Redirect(info, false);
        if (info == null) {
            info = parseM3U8Info();
            info = m3u8Redirect(info, false);
        }

        //判断一下文件的长度是否获取得到
        if (info == null) {
            info = getM3u8Response(httpRequest.getConnectUrl(), true);
        }
        if (info == null) {
            throw new ParseException("M3U8 info parse error!",0);
        }
        SerializeFactory.writeM3u8Info(httpRequest, info);
        //创建网络文件
        M3U8Utils.createNetM3U8(getM3u8NetFile(), info);
        OnM3u8ParseIntercept intercept = httpRequest.m3u8ParseIntercept();
        if (intercept != null) {
            intercept.intercept(info);
        }
        checkIsCancel();

        if (!usedMultiThread) {
            singleDownloadM3u8 = new SingleDownloadM3u8(httpRequest, this, info);
            singleDownloadM3u8.onExecute(httpCall);
        } else {
            multiThreadRun(info);
        }
    }


    /**
     * 检测是否已经下载完成
     *
     * @return
     */
    private boolean checkComplete() {
        return saveFile.exists() && saveFile.length() > 0;
    }

    /**
     * 多线程下载
     */
    private void multiThreadRun(M3U8Info info) {
        if (threadPoolExecutor != null) {
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

        threadPoolExecutor = CoreDownload.newSubTaskQueue(httpRequest.getDownloadMultiThreadSize());

        final CountDownLatch countDownLatch = new CountDownLatch(info.getTsList().size());//计数器
        final MultiM3u8Disposer disposer = new MultiM3u8Disposer(httpRequest, countDownLatch, saveFile, info);

        final List<Dispatcher> dispatcherList = new ArrayList<>();

        File filePath = saveFile();
        final ReentrantLock lock = new ReentrantLock();
        for (int i = 0; i < info.getTsList().size(); i++) {
            checkIsCancel();
            MultiM3u8Executor task = new MultiM3u8Executor(httpRequest,
                    filePath,
                    info,
                    tempCacheDir,
                    i,
                    disposer,
                    lock);
            dispatcherList.add(task);
            disposer.addTask(task);
            task.setTaskFuture(threadPoolExecutor.submit(task));
        }

        //等待下载完成
        try {
            countDownLatch.await();
            disposer.onFailure(this);
        } catch (InterruptedException e) {
            for (Dispatcher dispatcher : dispatcherList) {
                dispatcher.cancel();
            }
            throw new CancelTaskException();
        } finally {
            threadPoolExecutor.shutdownNow();
        }
    }

    //检测m3u8是否需要重定向
    private M3U8Info m3u8Redirect(M3U8Info info, boolean retry) throws Throwable {
        if (info != null && info.isNeedRedirect()) {
            return getM3u8Response(info.getUrl(), retry);
        }
        return info;
    }

    //检测本地的m3u8文件
    private M3U8Info parseM3U8Info() throws Exception {
        checkIsCancel();
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
     * 获取指定地址的响应结果
     *
     * @return false 需要重试
     * @throws Exception
     */
    private M3U8Info getM3u8Response(String baseUrl, boolean retry) throws Throwable {
        checkIsCancel();
        currentURL.set(baseUrl);
        Response response = httpCall.process(httpRequest.request(baseUrl));
        if (response.isSuccess()) {
            M3U8Info info = parseNetworkM3U8Info(baseUrl, new BufferedReader(new InputStreamReader(response.body().source())));
            return m3u8Redirect(info, retry);
        } else {
            if (retry) {
                //抛出错误
                HttpResponse httpResponse = new HttpResponse(response);
                String url = response.request().url().toString();
                throw new HttpErrorException(response.code(), url, httpResponse.error());
            }
            return null;
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
        OnM3u8ParseIntercept intercept = httpRequest.m3u8ParseIntercept();
        try {
            if (intercept != null) {
                M3U8Info info = intercept.intercept(reader);
                if (info != null) {
                    return info;
                }
            }
            return M3U8Utils.parseNetworkM3U8Info(baseUrl, reader);
        } finally {
            XDownUtils.closeIo(reader);
        }
    }

    /**
     * 保存m3u8网络信息的文件
     *
     * @return
     */
    private File getM3u8NetFile() {
        //保存m3u8信息
        return new File(saveFile.getAbsolutePath().replace(".m3u8","") + "_net.m3u8");
    }

    @Override
    protected void completeRun() {
        CoreDownload.removeExecute(tag(), this);
        httpCall.terminated();
    }

    @Override
    protected void applyCancel() {
        httpCall.cancel();
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdownNow();
        }
        if (singleDownloadM3u8 != null) {
            singleDownloadM3u8.cancel();
        }
    }

    @Override
    public File saveFile() {
        return httpRequest.getSaveFile();
    }

}
