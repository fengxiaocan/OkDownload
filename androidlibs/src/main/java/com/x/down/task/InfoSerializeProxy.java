package com.x.down.task;

import com.x.down.core.XDownloadRequest;
import com.x.down.m3u8.M3U8Info;
import com.x.down.made.DownloaderBlock;
import com.x.down.made.DownloaderInfo;
import com.x.down.tool.XDownUtils;

import java.io.File;

class InfoSerializeProxy {
    private static final String INFO_NAME = "MEMORY";
    private static final String BLOCK_NAME = "BLOCK";
    private static final String M3U8_INFO_NAME = "M3U8_BLOCK";

    /**
     * 保存下载的长度以及文件类型
     *
     * @param request
     * @param info
     */
    public static void writeDownloaderInfo(XDownloadRequest request, DownloaderInfo info) {
        File cacheDir = XDownUtils.getTempCacheDir(request,true);
        File file = new File(cacheDir, INFO_NAME);
        XDownUtils.writeObject(file, info);
    }

    /**
     * 获取下载的长度以及文件类型
     *
     * @param request
     */
    public static DownloaderInfo readDownloaderInfo(XDownloadRequest request) {
        File cacheDir = XDownUtils.getTempCacheDir(request,false);
        if (cacheDir.exists()) {
            File file = new File(cacheDir, INFO_NAME);
            return XDownUtils.readObject(file);
        }
        return null;
    }

    /**
     * 保存多线程下载的数量配置
     *
     * @param request
     * @param block
     */
    public static void writeDownloaderBlock(XDownloadRequest request, DownloaderBlock block) {
        File cacheDir = XDownUtils.getTempCacheDir(request,true);
        File file = new File(cacheDir, BLOCK_NAME);
        XDownUtils.writeObject(file, block);
    }

    /**
     * 获取多线程下载的数量配置
     */
    public static DownloaderBlock readDownloaderBlock(XDownloadRequest request) {
        File cacheDir = XDownUtils.getTempCacheDir(request,false);
        if (cacheDir.exists()) {
            File file = new File(cacheDir, BLOCK_NAME);
            return XDownUtils.readObject(file);
        }
        return null;
    }

    /**
     * 保存m3u8的信息
     */
    public static void deleteM3u8Info(XDownloadRequest request) {
        File tempCacheDir = XDownUtils.getTempCacheDir(request,false);
        new File(tempCacheDir, M3U8_INFO_NAME).delete();
    }

    /**
     * 保存m3u8的信息
     */
    public static void writeM3u8Info(XDownloadRequest request, M3U8Info block) {
        File tempCacheDir = XDownUtils.getTempCacheDir(request,true);
        File file = new File(tempCacheDir, M3U8_INFO_NAME);
        synchronized (Object.class) {
            XDownUtils.writeObject(file, block);
        }
    }

    /**
     * 获取m3u8的信息
     */
    public static M3U8Info readM3u8Info(XDownloadRequest request) {
        synchronized (Object.class) {
            File cacheDir = XDownUtils.getTempCacheDir(request,false);
            if (cacheDir.exists()) {
                File file = new File(cacheDir, M3U8_INFO_NAME);
                return XDownUtils.readObject(file);
            }
            return null;
        }
    }
}
