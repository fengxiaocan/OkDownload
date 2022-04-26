package com.x.down.task;

import com.x.down.core.XDownloadRequest;
import com.x.down.made.DownloaderBlock;
import com.x.down.made.DownloaderInfo;
import com.x.down.made.M3u8DownloaderBlock;
import com.x.down.made.M3u8DownloaderBlockInfo;
import com.x.down.made.M3u8DownloaderInfo;
import com.x.down.tool.XDownUtils;

import java.io.File;

class InfoSerializeProxy {
    private static final String INFO_NAME = "MEMORY";
    private static final String BLOCK_NAME = "BLOCK";

    /**
     * 保存下载的长度以及文件类型
     *
     * @param request
     * @param info
     */
    public static void writeDownloaderInfo(XDownloadRequest request, DownloaderInfo info) {
        File cacheDir = XDownUtils.getTempCacheDir(request);
        File file = new File(cacheDir, INFO_NAME);
        XDownUtils.writeObject(file, info);
    }

    /**
     * 获取下载的长度以及文件类型
     *
     * @param request
     */
    public static DownloaderInfo readDownloaderInfo(XDownloadRequest request) {
        File cacheDir = XDownUtils.getTempCacheDir(request);
        if (cacheDir.exists()) {
            File file = new File(cacheDir, INFO_NAME);
            if (file.exists()) {
                return XDownUtils.readObject(file);
            }
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
        File cacheDir = XDownUtils.getTempCacheDir(request);
        File file = new File(cacheDir, BLOCK_NAME);
        XDownUtils.writeObject(file, block);
    }

    /**
     * 获取多线程下载的数量配置
     */
    public static DownloaderBlock readDownloaderBlock(XDownloadRequest request) {
        File cacheDir = XDownUtils.getTempCacheDir(request);
        if (cacheDir.exists()) {
            File file = new File(cacheDir, BLOCK_NAME);
            if (file.exists()) {
                return XDownUtils.readObject(file);
            }
        }
        return null;
    }

    /**
     * 保存m3u8的信息
     */
    public static void writeM3u8DownloadInfo(XDownloadRequest request, M3u8DownloaderInfo block) {
        File tempCacheDir = XDownUtils.getTempCacheDir(request);
        File file = new File(tempCacheDir, BLOCK_NAME);
        XDownUtils.writeObject(file, block);
    }

    /**
     * 获取m3u8的信息
     */
    public static M3u8DownloaderInfo readM3u8DownloadInfo(XDownloadRequest request) {
        File cacheDir = XDownUtils.getTempCacheDir(request);
        if (cacheDir.exists()) {
            File file = new File(cacheDir, BLOCK_NAME);
            if (file.exists()) {
                return XDownUtils.readObject(file);
            }
        }
        return null;
    }

    /**
     * 保存m3u8片段的信息
     */
    public static void writeM3u8DownloaderBlock(XDownloadRequest request, M3u8DownloaderBlock block, long contentLength) {
        File tempCacheDir = XDownUtils.getTempCacheDir(request);
        String md5 = XDownUtils.getMd5(block.getUrl());
        File file = new File(tempCacheDir, md5 + ".m3u8block");
        XDownUtils.writeObject(file, new M3u8DownloaderBlockInfo(block, contentLength));
    }

    /**
     * 获取m3u8片段的信息
     */
    public static long readM3u8DownloaderBlock(XDownloadRequest request, M3u8DownloaderBlock block) {
        File tempCacheDir = XDownUtils.getTempCacheDir(request);
        if (tempCacheDir.exists()) {
            String md5 = XDownUtils.getMd5(block.getUrl());
            File file = new File(tempCacheDir, md5 + ".m3u8block");
            if (file.exists()) {
                M3u8DownloaderBlockInfo info = XDownUtils.readObject(file);
                if (info != null) {
                    return info.getContentLength();
                }
            }
        }
        return 0;
    }
}
