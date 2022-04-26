package com.x.down.listener;

import com.x.down.base.IDownloadRequest;

public interface OnSpeedListener {
    void onSpeed(IDownloadRequest request, int speed, int time);
}
