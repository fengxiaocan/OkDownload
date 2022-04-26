package com.x.test;

import com.x.down.XDownload;
import com.x.down.base.IDownloadRequest;
import com.x.down.listener.OnDownloadListener;
import com.x.down.listener.OnProgressListener;

public class Test {
    public static void main(String[] args) {
        XDownload.download("http://file0204.daimg.com/2020/2009/DAimG_2020091600079451KPKM.rar")
                .setUseMultiThread(true)
                .setDownloadMultiThreadSize(10)
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(IDownloadRequest request, float progress) {
                        System.out.println("onProgress=" + (int) (100 * progress));
                    }
                })
                .setDownloadListener(new OnDownloadListener() {
                    @Override
                    public void onComplete(IDownloadRequest iDownloadRequest) {
                        System.out.println("onComplete");
                    }

                    @Override
                    public void onFailure(IDownloadRequest iDownloadRequest) {
                        System.out.println("onFailure");
                    }
                }).start();
    }
}
