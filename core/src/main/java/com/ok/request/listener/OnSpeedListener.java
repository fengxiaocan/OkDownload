package com.ok.request.listener;

import com.ok.request.base.DownloadExecutor;

public interface OnSpeedListener {
    void onSpeed(DownloadExecutor executor, int speed, int time);
}
