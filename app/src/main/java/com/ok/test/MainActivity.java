package com.ok.test;


import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ok.request.OkDownload;
import com.ok.request.base.DownloadExecutor;
import com.ok.request.base.Execute;
import com.ok.request.listener.OnDownloadListener;
import com.ok.request.listener.OnExecuteQueueListener;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    static Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OkDownload.download("https://media6.smartstudy.com/ae/07/3997/2/dest.m3u8")
                .setOnProgressListener((request, progress, totalLength, downloadedLength) -> {
                    Log.e("noah", "onProgress:" + progress);
                })
                .setDownloadListener(new OnDownloadListener() {
                    @Override
                    public void onComplete(DownloadExecutor request) {

                    }

                    @Override
                    public void onFailure(DownloadExecutor request) {

                    }
                }).start();

//        XDownload.execute(new Execute() {
//            @Override
//            public void run() throws Throwable {
//
//            }
//        }).start();
//        XDownload.download("http://file0204.daimg.com/2020/2009/DAimG_2020091600079451KPKM.rar")
//                .setUseMultiThread(true)
//                .setDownloadMultiThreadSize(20)
//                .setOnProgressListener(new OnProgressListener() {
//                    @Override
//                    public void onProgress(IDownloadRequest request, float progress) {
//                        Log.e("noah","onProgress=" + (int) (100 * progress));
//                    }
//                })
//                .setDownloadListener(new OnDownloadListener() {
//                    @Override
//                    public void onComplete(IDownloadRequest iDownloadRequest) {
//                        Log.e("noah","onComplete");
//                    }
//
//                    @Override
//                    public void onFailure(IDownloadRequest iDownloadRequest) {
//                        Log.e("noah","onFailure");
//                    }
//                }).start();
//        XDownload.executes(new Run(1))
//                .setMaxExecuteTaskCount(10)
//                .setExecuteQueueListener(getListener())
//                .addRequest(new Run(2))
//                .addRequest(new Run(3))
//                .addRequest(new Run(4))
//                .addRequest(new Run(5))
//                .addRequest(new Run(6))
//                .then(new Run(11))
//                .setExecuteQueueListener(getListener())
//                .addRequest(new Run(12))
//                .addRequest(new Run(12))
//                .addRequest(new Run(13))
//                .addRequest(new Run(14))
//                .addRequest(new Run(15))
//                .addRequest(new Run(16))
//                .then(new Run(21))
//                .setExecuteQueueListener(getListener())
//                .addRequest(new Run(22))
//                .addRequest(new Run(22))
//                .addRequest(new Run(23))
//                .addRequest(new Run(24))
//                .addRequest(new Run(25))
//                .addRequest(new Run(26))
//                .start();
    }

    @NonNull
    private OnExecuteQueueListener getListener() {
        return new OnExecuteQueueListener() {
            @Override
            public void onComplete(int taskCount, int completeCount) {
                Log.e("noah", "taskCount=" + taskCount + " completeCount=" + completeCount);
            }
        };
    }

    static class Run implements Execute {
        private int index;

        public Run(int index) {
            this.index = index;
        }

        @Override
        public void run() throws Throwable {
            Log.e("noah", "开始运行=" + index);
            SystemClock.sleep((random.nextInt(5) + 2) * 1000);
            Log.e("noah", "运行完成>>>=" + index);
        }
    }

}
