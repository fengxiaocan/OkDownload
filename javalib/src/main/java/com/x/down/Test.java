package com.x.down;

import com.x.down.base.IDownloadRequest;
import com.x.down.listener.OnDownloadListener;
import com.x.down.listener.OnSpeedListener;

import java.io.File;

public class Test {
    public static void main(String[] args) {
        File file = new File(".", "a.php");
        XDownload.download("")
                .parseM3u8(file)
                .setOnSpeedListener(new OnSpeedListener() {
                    @Override
                    public void onSpeed(IDownloadRequest iDownloadRequest, int i, int i1) {
                        System.err.println("speed="+i);
                    }
                })
                .setDownloadListener(new OnDownloadListener() {
                    @Override
                    public void onComplete(IDownloadRequest iDownloadRequest) {

                    }

                    @Override
                    public void onFailure(IDownloadRequest iDownloadRequest, Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .start();
    }
}
