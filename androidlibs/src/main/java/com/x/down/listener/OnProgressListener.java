package com.x.down.listener;

import com.x.down.base.IDownloadRequest;

public interface OnProgressListener {
    void onProgress(IDownloadRequest request, float progress);
}
