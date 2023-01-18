package com.ok.request.listener;

import com.ok.request.base.DownloadExecutor;

public interface OnProgressListener {
    void onProgress(DownloadExecutor executor, float progress, long totalLength, long sofarLength);
}
