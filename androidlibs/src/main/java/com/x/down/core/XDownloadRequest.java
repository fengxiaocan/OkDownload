package com.x.down.core;

import com.x.down.XDownload;
import com.x.down.config.AcquireNameInterceptor;
import com.x.down.config.Config;
import com.x.down.data.Headers;
import com.x.down.data.Params;
import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnDownloadConnectListener;
import com.x.down.listener.OnDownloadListener;
import com.x.down.listener.OnMergeFileListener;
import com.x.down.listener.OnProgressListener;
import com.x.down.listener.OnSpeedListener;
import com.x.down.listener.SSLCertificateFactory;
import com.x.down.task.ThreadTaskFactory;
import com.x.down.tool.XDownUtils;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class XDownloadRequest extends BaseRequest implements HttpDownload, BuilderURLConnection {
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
    //下载过程连接监听
    protected OnDownloadConnectListener onDownloadConnectListener;
    //下载进度监听
    protected OnProgressListener onProgressListener;
    //下载速度监听
    protected OnSpeedListener onSpeedListener;
    //文件合并监听
    protected OnMergeFileListener onMergeFileListener;

    protected XDownloadRequest(String baseUrl) {
        super(baseUrl);
        urlName = XDownUtils.getUrlName(getBaseUrl());
        fileSuffix = XDownUtils.getSuffixName(getUrlName()).toLowerCase();
    }

    public static XDownloadRequest with(String url) {
        return new XDownloadRequest(url);
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

    public String getSaveName() {
        if (XDownUtils.isEmpty(saveName)) {
            AcquireNameInterceptor interceptor = Config.config().getAcquireNameInterceptor();
            if (interceptor != null) {
                saveName = interceptor.acquire(getConnectUrl());
            }
            if (XDownUtils.isEmpty(saveName)) {
                saveName = getIdentifier() + "_" + getUrlName();
            }
        }
        return saveName;
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
    public HttpDownload setConnectListener(OnDownloadConnectListener listener) {
        onDownloadConnectListener = listener;
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
    public HttpDownload setOnMegerFileListener(OnMergeFileListener listener) {
        onMergeFileListener = listener;
        return this;
    }

    @Override
    public HttpDownload delete() {
        XDownload.get().cancleDownload(getTag());

        File saveFile = getSaveFile();
        if (saveFile.exists()) {
            saveFile.delete();
        }
        File tempFile = XDownUtils.getTempFile(this);
        if (tempFile.exists()) {
            tempFile.delete();
        }
        File tempCacheDir = XDownUtils.getTempCacheDir(this);
        XDownUtils.deleteDir(tempCacheDir);
        return this;
    }

    @Override
    public HttpDownload setTag(String tag) {
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
    public HttpDownload setConnectTimeOut(int connectTimeOut) {
        return (HttpDownload) super.setConnectTimeOut(connectTimeOut);
    }

    @Override
    public HttpDownload setIOTimeOut(int iOTimeOut) {
        return (HttpDownload) super.setIOTimeOut(iOTimeOut);
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

    public OnDownloadListener getOnDownloadListener() {
        return onDownloadListener;
    }

    public OnDownloadConnectListener getOnDownloadConnectListener() {
        return onDownloadConnectListener;
    }

    public OnProgressListener getOnProgressListener() {
        return onProgressListener;
    }

    public OnSpeedListener getOnSpeedListener() {
        return onSpeedListener;
    }

    public OnMergeFileListener getOnMegerFileListener() {
        return onMergeFileListener;
    }

    @Override
    public String start() {
        if (getConnectUrl().endsWith(".m3u8")) {
            ThreadTaskFactory.createM3u8DownloaderRequest(this);
        } else {
            if (isUseMultiThread && downloadMultiThreadSize > 1) {
                ThreadTaskFactory.createDownloadThreadRequest(this);
            } else {
                ThreadTaskFactory.createSingleDownloadTask(this);
            }
        }
        return getTag();
    }

    @Override
    public HttpURLConnection buildConnect(String connectUrl) throws Exception {
        URL url = new URL(connectUrl);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();

        if (http instanceof HttpsURLConnection) {
            disposeCertificate(http);
        }

        http.setRequestMethod("GET");
        //设置http请求头
        http.setRequestProperty("Connection", "Keep-Alive");
        if (headers != null) {
            for (String key : headers.keySet()) {
                http.setRequestProperty(key, headers.getValue(key));
            }
        }
        if (userAgent != null) {
            http.setRequestProperty("User-Agent", userAgent);
        }

        http.setConnectTimeout(Math.max(connectTimeOut, 5 * 1000));
        http.setReadTimeout(Math.max(iOTimeOut, 5 * 1000));

        //本次链接自动处理重定向
        http.setInstanceFollowRedirects(true);

        http.setUseCaches(false);
        http.setDoInput(true);
        return http;
    }

    public HttpURLConnection buildConnect() throws Exception {
        return buildConnect(getConnectUrl());
    }
}
