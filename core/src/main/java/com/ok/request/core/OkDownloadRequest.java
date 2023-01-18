package com.ok.request.core;

import com.ok.request.CoreDownload;
import com.ok.request.base.Call;
import com.ok.request.base.DownloadExecutor;
import com.ok.request.base.HttpDownload;
import com.ok.request.call.Interceptor;
import com.ok.request.config.AcquireNameInterceptor;
import com.ok.request.config.Config;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.dispatch.OnDownloadDispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.factory.ThreadTaskFactory;
import com.ok.request.listener.OnDownloadListener;
import com.ok.request.listener.OnExecuteListener;
import com.ok.request.listener.OnM3u8ParseIntercept;
import com.ok.request.listener.OnMergeM3u8Listener;
import com.ok.request.listener.OnProgressListener;
import com.ok.request.listener.OnSpeedListener;
import com.ok.request.listener.SSLCertificateFactory;
import com.ok.request.params.Headers;
import com.ok.request.params.Params;
import com.ok.request.request.DownloadRequest;
import com.ok.request.request.Request;
import com.ok.request.tool.XDownUtils;

import java.io.File;
import java.util.UUID;

import javax.net.ssl.SSLSocketFactory;

public class OkDownloadRequest extends BaseRequest implements HttpDownload, OnDownloadDispatcher {
    //文件的后缀名
    protected final String fileSuffix;
    protected final String urlName;
    //文件名
    protected String saveName;
    //文件保存位置
    protected String saveDir = Config.config().getSaveDir();
    //文件缓存目录
    protected String cacheDir = Config.config().getCacheDir();
    //写文件buff大小，该数值大小不能小于2048，数值变小，下载速度会变慢,默认8kB
    protected int bufferedSize = Config.config().getBufferedSize();
    //默认下载的多线程数
    protected int downloadMultiThreadSize = Config.config().getDownloadMultiThreadSize();
    //默认多线程下载的单线程最大下载文件块大小,默认10MB
    protected int maxDownloadBlockSize = Config.config().getMaxDownloadBlockSize();
    //默认多线程下载的单线程最小下载文件块大小,默认100KB
    protected int minDownloadBlockSize = Config.config().getMinDownloadBlockSize();
    //是否使用多线程下载
    protected boolean isUseMultiThread = Config.config().isUseMultiThread();
    //是否使用断点续传
    protected boolean isUseBreakpointResume = Config.config().isUseBreakpointResume();
    //是否忽略下载的progress回调
    protected boolean ignoredProgress = Config.config().isIgnoredProgress();
    //是否忽略下载的progress回调
    protected boolean ignoredSpeed = Config.config().isIgnoredSpeed();
    //更新进度条的间隔
    protected int updateProgressTimes = Config.config().getUpdateProgressTimes();
    //更新下载速度的间隔
    protected int updateSpeedTimes = Config.config().getUpdateSpeedTimes();
    //下载完成失败监听
    protected OnDownloadListener onDownloadListener;
    //下载进度监听
    protected OnProgressListener onProgressListener;
    //下载速度监听
    protected OnSpeedListener onSpeedListener;
    //文件合并监听
    protected OnMergeM3u8Listener onMergeM3u8Listener;
    //m3u8解析拦截器
    protected OnM3u8ParseIntercept onM3u8ParseIntercept;
    //是否强制为m3u8
    protected boolean asM3u8 = false;
    //m3u8信息的文件路径
    protected String m3u8Path;
    //m3u8的信息
    protected String m3u8Info;

    protected OkDownloadRequest(String baseUrl) {
        super(baseUrl);
        urlName = XDownUtils.getUrlName(baseUrl);
        fileSuffix = XDownUtils.getSuffixName(getUrlName()).toLowerCase();
    }

    public static OkDownloadRequest with(String url) {
        return new OkDownloadRequest(url);
    }

    public String getUrlName() {
        return urlName;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public File getSaveFile() {
        return new File(getSaveDir(), getSaveName());
    }

    @Override
    public String getIdentifier() {
        if (isAsM3u8()) {
            if (XDownUtils.isEmpty(getConnectUrl())) {
                if (!XDownUtils.isEmpty(m3u8Info)) {
                    return XDownUtils.getMd5(m3u8Info);
                }else if (!XDownUtils.isEmpty(m3u8Path)) {
                    return XDownUtils.getMd5(m3u8Path);
                }else {
                    return UUID.randomUUID().toString();
                }
            }
        }
        return super.getIdentifier();
    }

    public String getSaveName() {
        if (XDownUtils.isEmpty(saveName)) {
            AcquireNameInterceptor interceptor = Config.config().getAcquireNameInterceptor();
            if (interceptor != null) {
                saveName = interceptor.acquire(getConnectUrl());
            }
            if (XDownUtils.isEmpty(saveName)) {
                if (isAsM3u8()) {
                    saveName = getIdentifier() + ".m3u8";
                }else {
                    saveName = getIdentifier() + "_" + getUrlName();
                }
            }
        }
        return saveName;
    }

    public String getM3u8DirName() {
        return getSaveName().replace(".m3u8", "");
    }

    public File getM3u8Dir() {
        return new File(getSaveDir(), getM3u8DirName());
    }

    public String getSaveDir() {
        if (XDownUtils.isEmpty(saveDir)) {
            return Config.config().getSaveDir();
        }
        return saveDir;
    }

    public String getCacheDir() {
        if (XDownUtils.isEmpty(cacheDir)) {
            return Config.config().getCacheDir();
        }
        return cacheDir;
    }

    @Override
    public HttpDownload setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
        return this;
    }

    @Override
    public HttpDownload setIgnoredProgress(boolean ignoredProgress) {
        this.ignoredProgress = ignoredProgress;
        return this;
    }


    @Override
    public HttpDownload setIgnoredSpeed(boolean ignoredSpeed) {
        this.ignoredSpeed = ignoredSpeed;
        return this;
    }

    @Override
    public HttpDownload setUpdateProgressTimes(int updateProgressTimes) {
        this.updateProgressTimes = updateProgressTimes;
        return this;
    }

    @Override
    public HttpDownload setUpdateSpeedTimes(int updateSpeedTimes) {
        this.updateSpeedTimes = updateSpeedTimes;
        return this;
    }

    @Override
    public HttpDownload setUseMultiThread(boolean useMultiThread) {
        this.isUseMultiThread = useMultiThread;
        return this;
    }

    @Override
    public HttpDownload setBufferedSize(int bufferedSize) {
        this.bufferedSize = bufferedSize;
        return this;
    }

    @Override
    public HttpDownload setDownloadMultiThreadSize(int multiThreadCount) {
        this.downloadMultiThreadSize = multiThreadCount;
        return this;
    }

    @Override
    public HttpDownload setMaxDownloadBlockSize(int size) {
        this.maxDownloadBlockSize = size;
        return this;
    }

    @Override
    public HttpDownload setMinDownloadBlockSize(int size) {
        this.minDownloadBlockSize = size;
        return this;
    }

    @Override
    public HttpDownload setUseBreakpointResume(boolean useBreakpointResume) {
        this.isUseBreakpointResume = useBreakpointResume;
        return this;
    }

    @Override
    public HttpDownload setDownloadListener(OnDownloadListener listener) {
        onDownloadListener = listener;
        return this;
    }

    @Override
    public HttpDownload setOnProgressListener(OnProgressListener listener) {
        onProgressListener = listener;
        return this;
    }

    @Override
    public HttpDownload setOnSpeedListener(OnSpeedListener listener) {
        onSpeedListener = listener;
        return this;
    }

    @Override
    public HttpDownload setOnMegerM3u8Listener(OnMergeM3u8Listener listener) {
        onMergeM3u8Listener = listener;
        return this;
    }

    @Override
    public HttpDownload setOnM3u8ParseIntercept(OnM3u8ParseIntercept listener) {
        onM3u8ParseIntercept = listener;
        return this;
    }

    @Override
    public HttpDownload delete() {
        CoreDownload.cancelExecute(getTag());

        File saveFile = getSaveFile();
        saveFile.deleteOnExit();

        File tempFile = XDownUtils.getTempFile(this);
        tempFile.deleteOnExit();

        File tempCacheDir = XDownUtils.getTempCacheDir(this);
        XDownUtils.deleteDir(tempCacheDir);
        return this;
    }

    @Override
    public HttpDownload asM3u8() {
        this.asM3u8 = true;
        return this;
    }

    @Override
    public HttpDownload parseM3u8(File fileM3u8) {
        this.m3u8Path = fileM3u8.getAbsolutePath();
        this.asM3u8 = true;
        return this;
    }

    @Override
    public HttpDownload parseM3u8Path(String fileM3u8) {
        this.m3u8Path = fileM3u8;
        this.asM3u8 = true;
        return this;
    }

    @Override
    public HttpDownload parseM3u8Info(String m3u8Info) {
        this.m3u8Info = m3u8Info;
        this.asM3u8 = true;
        return this;
    }

    @Override
    public HttpDownload setTag(Object tag) {
        return (HttpDownload) super.setTag(tag);
    }


    @Override
    public HttpDownload setSSLCertificate(String path) {
        return (HttpDownload) super.setSSLCertificate(path);
    }

    @Override
    public HttpDownload setSSLCertificateFactory(SSLCertificateFactory factory) {
        return (HttpDownload) super.setSSLCertificateFactory(factory);
    }

    @Override
    public HttpDownload setFileName(String name) {
        this.saveName = name;
        return this;
    }

    @Override
    public HttpDownload setFileDir(String dir) {
        this.saveDir = dir;
        return this;
    }

    @Override
    public HttpDownload setFileDir(File dir) {
        this.saveDir = dir.getAbsolutePath();
        return this;
    }

    @Override
    public HttpDownload setSaveFile(String saveFile) {
        setSaveFile(new File(saveFile));
        return this;
    }

    @Override
    public HttpDownload setSaveFile(File saveFile) {
        this.saveDir = saveFile.getParent();
        this.saveName = saveFile.getName();
        return this;
    }

    @Override
    public HttpDownload addParams(String name, String value) {
        return (HttpDownload) super.addParams(name, value);
    }

    @Override
    public HttpDownload addHeader(String name, String value) {
        return (HttpDownload) super.addHeader(name, value);
    }

    @Override
    public HttpDownload setParams(Params params) {
        return (HttpDownload) super.setParams(params);
    }

    @Override
    public HttpDownload setHeader(Headers header) {
        return (HttpDownload) super.setHeader(header);
    }

    @Override
    public HttpDownload setUserAgent(String userAgent) {
        return (HttpDownload) super.setUserAgent(userAgent);
    }

    @Override
    public HttpDownload setTimeOut(int iOTimeOut) {
        return (HttpDownload) super.setTimeOut(iOTimeOut);
    }

    @Override
    public HttpDownload setUseAutoRetry(boolean useAutoRetry) {
        return (HttpDownload) super.setUseAutoRetry(useAutoRetry);
    }

    @Override
    public HttpDownload setAutoRetryTimes(int autoRetryTimes) {
        return (HttpDownload) super.setAutoRetryTimes(autoRetryTimes);
    }

    @Override
    public HttpDownload setAutoRetryInterval(int autoRetryInterval) {
        return (HttpDownload) super.setAutoRetryInterval(autoRetryInterval);
    }

    @Override
    public HttpDownload permitAllSslCertificate(boolean wifiRequired) {
        return (HttpDownload) super.permitAllSslCertificate(wifiRequired);
    }

    @Override
    public HttpDownload scheduleOn(Schedulers schedulers) {
        return (HttpDownload) super.scheduleOn(schedulers);
    }

    @Override
    public HttpDownload setExecuteListener(OnExecuteListener executeListener) {
        return (HttpDownload) super.setExecuteListener(executeListener);
    }

    @Override
    public HttpDownload setNetworkInterceptors(Interceptor interceptor) {
        return (HttpDownload) super.setNetworkInterceptors(interceptor);
    }

    @Override
    public HttpDownload setInterceptors(Call.Interceptor interceptor) {
        return (HttpDownload) super.setInterceptors(interceptor);
    }

    public int getBufferedSize() {
        return bufferedSize;
    }

    public int getDownloadMultiThreadSize() {
        return downloadMultiThreadSize;
    }

    public int getMaxDownloadBlockSize() {
        return maxDownloadBlockSize;
    }

    public int getMinDownloadBlockSize() {
        return minDownloadBlockSize;
    }

    public boolean isUseMultiThread() {
        return isUseMultiThread;
    }

    public boolean isUseBreakpointResume() {
        return isUseBreakpointResume;
    }

    public boolean isIgnoredProgress() {
        return ignoredProgress;
    }

    public boolean isIgnoredSpeed() {
        return ignoredSpeed;
    }

    public int getUpdateProgressTimes() {
        return updateProgressTimes;
    }

    public int getUpdateSpeedTimes() {
        return updateSpeedTimes;
    }


    public boolean isAsM3u8() {
        return asM3u8;
    }

    public String getM3u8Path() {
        return m3u8Path;
    }

    public String getM3u8Info() {
        return m3u8Info;
    }

    @Override
    public Dispatcher start() {
        if (getConnectUrl().endsWith(".m3u8") || asM3u8) {
            return ThreadTaskFactory.createM3u8DownloaderRequest(this);
        } else {
            return ThreadTaskFactory.createDownloadThreadRequest(this);
        }
    }

    public boolean isUsedMultiThread() {
        return isUseMultiThread && downloadMultiThreadSize > 1;
    }

    @Override
    public Request request() throws Exception {
        return request(getConnectUrl());
    }

    @Override
    public Request request(String url) throws Exception {
        DownloadRequest request = new DownloadRequest(url);
        request.setHeaders(headers);
        request.TimeOut(iOTimeOut);
        //处理https证书
        SSLSocketFactory certificate = null;
        if (sslCertificateFactory != null) {
            certificate = sslCertificateFactory.createCertificate();
        } else if (certificatePath != null) {
            certificate = XDownUtils.getCertificate(certificatePath);
        } else if (permitAllSslCertificate) {
            //允许所有的https证书
            if (Config.ANDROID_SDK_VER29) {
                certificate = XDownUtils.getUnSafeCertificate();
            }
        }
        request.certificate(certificate);
        return request;
    }

    @Override
    public OnProgressListener progressListener() {
        return onProgressListener;
    }

    @Override
    public OnSpeedListener speedListener() {
        return onSpeedListener;
    }

    @Override
    public OnMergeM3u8Listener mergeM3u8Listener() {
        return onMergeM3u8Listener;
    }

    @Override
    public OnM3u8ParseIntercept m3u8ParseIntercept() {
        return onM3u8ParseIntercept;
    }

    //完成回调
    public void callDownloadComplete(final DownloadExecutor executor) {
        //完成回调
        if (onDownloadListener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        onDownloadListener.onComplete(executor);
                    }
                });
            } else {
                onDownloadListener.onComplete(executor);
            }
        }
    }

    //完成回调
    public void callDownloadFailure(final DownloadExecutor executor) {

        if (onDownloadListener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        onDownloadListener.onFailure(executor);
                    }
                });
            } else {
                onDownloadListener.onFailure(executor);
            }
        }
    }
}
